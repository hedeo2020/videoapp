import { createHash, randomUUID } from "node:crypto";
import Fastify, { type FastifyReply, type FastifyRequest } from "fastify";
import helmet from "@fastify/helmet";
import cors from "@fastify/cors";
import cookie from "@fastify/cookie";
import rateLimit from "@fastify/rate-limit";
import { Prisma, PrismaClient, type Role } from "@prisma/client";
import argon2 from "argon2";
import { z } from "zod";
import { config } from "./config.js";
import { isAdminRole, issueSession, publicUser, rotateSession, verifyPasswordAndTrack } from "./auth.js";
import { hashToken, opaqueToken, signAccess, signPlayback, verifyAccess } from "./security.js";
import { hasPermission } from "./rbac.js";

const prisma=new PrismaClient();
const app=Fastify({logger:{level:config.NODE_ENV==="production"?"info":"debug",redact:["req.headers.authorization","req.headers.cookie","res.headers.set-cookie","body.password","body.token","body.refreshToken"]},genReqId:()=>randomUUID(),trustProxy:config.NODE_ENV==="production"?1:false});
await app.register(helmet,{global:true});
await app.register(cors,{origin:config.ADMIN_ORIGIN,credentials:true,methods:["GET","POST","PATCH","DELETE"]});
await app.register(cookie);
await app.register(rateLimit,{max:100,timeWindow:"1 minute"});

type Auth={sub:string;role:Role};
type AuthRequest=FastifyRequest & {auth:Auth};
const ipHash=(req:FastifyRequest)=>createHash("sha256").update(req.ip).digest("hex");
const sessionContext=(req:FastifyRequest,deviceId:string)=>({deviceId,userAgent:req.headers["user-agent"],ipHash:ipHash(req)});
const sendError=(reply:FastifyReply,requestId:string,status:number,code:string,message:string)=>reply.code(status).send({error:{code,message,requestId}});

app.setErrorHandler((error,req,reply)=>{const normalized=error as {statusCode?:number;message?:string;name?:string}; const validation=normalized.name==="ZodError"; const status=validation?400:normalized.statusCode??500; req.log.error({err:error},"request failed"); reply.status(status).send({error:{code:validation?"VALIDATION_ERROR":status===500?"INTERNAL_ERROR":"REQUEST_ERROR",message:status===500?"Unexpected server error":normalized.message??"Request failed",requestId:req.id}})});

async function authenticate(req:FastifyRequest,reply:FastifyReply){const bearer=req.headers.authorization?.match(/^Bearer (.+)$/)?.[1]; const cookieToken=req.cookies[config.ADMIN_COOKIE_NAME]; const raw=bearer??cookieToken; if(!raw)return sendError(reply,req.id,401,"UNAUTHENTICATED","Authentication required"); try{const payload=await verifyAccess(raw); if(!payload.sub||typeof payload.role!=="string")throw new Error("invalid claims"); (req as AuthRequest).auth={sub:payload.sub,role:payload.role as Role};}catch{return sendError(reply,req.id,401,"TOKEN_INVALID","Session expired")}}
const requirePermission=(permission:string)=>async(req:FastifyRequest,reply:FastifyReply)=>{await authenticate(req,reply);if(reply.sent)return; if(!hasPermission((req as AuthRequest).auth.role,permission))return sendError(reply,req.id,403,"FORBIDDEN","Insufficient permission")};
async function requireCsrf(req:FastifyRequest,reply:FastifyReply){const header=req.headers["x-csrf-token"];const cookieValue=req.cookies[config.ADMIN_CSRF_COOKIE_NAME];if(!header||header!==cookieValue)return sendError(reply,req.id,403,"CSRF_INVALID","CSRF validation failed")}
async function audit(req:AuthRequest,action:string,targetType:string,targetId?:string,metadata?:Prisma.InputJsonValue){await prisma.adminAuditLog.create({data:{actorUserId:req.auth.sub,action,targetType,targetId,requestId:req.id,ipHash:ipHash(req),metadata}})}

app.get("/health",async()=>({status:"ok"}));
app.get("/ready",async(_req,reply)=>{try{await prisma.$queryRaw`SELECT 1`;return {status:"ready"}}catch{return reply.code(503).send({status:"not-ready"})}});

app.post("/api/v1/auth/register",{config:{rateLimit:{max:5,timeWindow:"15 minutes"}}},async(req,reply)=>{
  if(!config.REGISTRATION_ENABLED)return sendError(reply,req.id,403,"REGISTRATION_DISABLED","Registration is currently unavailable");
  const body=z.object({email:z.string().email(),password:z.string().min(12).max(128),displayName:z.string().min(2).max(80)}).parse(req.body);
  const email=body.email.toLowerCase(); if(await prisma.user.findUnique({where:{email}}))return sendError(reply,req.id,409,"ACCOUNT_EXISTS","An account already exists");
  const user=await prisma.user.create({data:{email,displayName:body.displayName,passwordHash:await argon2.hash(body.password,{type:argon2.argon2id}),role:"VIEWER"}});
  const token=opaqueToken(); await prisma.emailVerificationToken.create({data:{userId:user.id,tokenHash:hashToken(token),expiresAt:new Date(Date.now()+24*60*60_000)}});
  await prisma.securityEvent.create({data:{userId:user.id,kind:"ACCOUNT_REGISTERED",severity:"INFO"}});
  return reply.code(201).send({user:publicUser(user),verificationToken:config.NODE_ENV==="development"?token:undefined});
});

app.post("/api/v1/auth/login",{config:{rateLimit:{max:8,timeWindow:"15 minutes"}}},async(req,reply)=>{
  const body=z.object({email:z.string().email(),password:z.string(),deviceId:z.string().min(8)}).parse(req.body);
  const user=await verifyPasswordAndTrack(prisma,body.email,body.password);
  if(!user||user.status!=="ACTIVE"){await prisma.securityEvent.create({data:{userId:user?.id,kind:"LOGIN_FAILED",severity:"MEDIUM",metadata:{ipHash:ipHash(req)}}});return sendError(reply,req.id,401,"INVALID_CREDENTIALS","Email or password is incorrect")}
  const tokens=await issueSession(prisma,user,sessionContext(req,body.deviceId));
  await prisma.securityEvent.create({data:{userId:user.id,kind:"LOGIN_SUCCEEDED",severity:"INFO"}});
  return {...tokens,user:publicUser(user)};
});

app.post("/api/v1/auth/refresh",async(req,reply)=>{const body=z.object({refreshToken:z.string().min(32),deviceId:z.string().min(8)}).parse(req.body);const result=await rotateSession(prisma,body.refreshToken,sessionContext(req,body.deviceId));if(result.kind!=="ok")return sendError(reply,req.id,401,result.kind==="reuse"?"TOKEN_REUSE":"REFRESH_INVALID","Session is no longer valid");return result});
app.post("/api/v1/auth/logout",async(req,reply)=>{const body=z.object({refreshToken:z.string().min(32)}).parse(req.body);await prisma.refreshSession.updateMany({where:{tokenHash:hashToken(body.refreshToken),revokedAt:null},data:{revokedAt:new Date()}});return reply.code(204).send()});

app.post("/api/v1/auth/forgot-password",{config:{rateLimit:{max:3,timeWindow:"15 minutes"}}},async(req)=>{const body=z.object({email:z.string().email()}).parse(req.body);const user=await prisma.user.findUnique({where:{email:body.email.toLowerCase()}});let token:string|undefined;if(user&&user.status==="ACTIVE"){token=opaqueToken();await prisma.passwordResetToken.create({data:{userId:user.id,tokenHash:hashToken(token),expiresAt:new Date(Date.now()+config.EMAIL_TOKEN_TTL_MINUTES*60_000)}})}return {message:"If the account exists, reset instructions will be sent.",resetToken:config.NODE_ENV==="development"?token:undefined}});
app.post("/api/v1/auth/reset-password",async(req,reply)=>{const body=z.object({token:z.string().min(32),password:z.string().min(12).max(128)}).parse(req.body);const record=await prisma.passwordResetToken.findUnique({where:{tokenHash:hashToken(body.token)}});if(!record||record.usedAt||record.expiresAt<=new Date())return sendError(reply,req.id,400,"RESET_INVALID","Reset token is invalid or expired");await prisma.$transaction([prisma.user.update({where:{id:record.userId},data:{passwordHash:await argon2.hash(body.password,{type:argon2.argon2id}),failedLoginCount:0,lockedUntil:null}}),prisma.passwordResetToken.update({where:{id:record.id},data:{usedAt:new Date()}}),prisma.refreshSession.updateMany({where:{userId:record.userId,revokedAt:null},data:{revokedAt:new Date()}})]);return {message:"Password updated"}});
app.post("/api/v1/auth/verify-email",async(req,reply)=>{const body=z.object({token:z.string().min(32)}).parse(req.body);const record=await prisma.emailVerificationToken.findUnique({where:{tokenHash:hashToken(body.token)}});if(!record||record.usedAt||record.expiresAt<=new Date())return sendError(reply,req.id,400,"VERIFICATION_INVALID","Verification token is invalid or expired");await prisma.$transaction([prisma.user.update({where:{id:record.userId},data:{emailVerifiedAt:new Date()}}),prisma.emailVerificationToken.update({where:{id:record.id},data:{usedAt:new Date()}})]);return {message:"Email verified"}});

app.get("/api/v1/account/me",{preHandler:authenticate},async(req)=>publicUser(await prisma.user.findUniqueOrThrow({where:{id:(req as AuthRequest).auth.sub}})));
app.get("/api/v1/account/sessions",{preHandler:authenticate},async(req)=>prisma.refreshSession.findMany({where:{userId:(req as AuthRequest).auth.sub,revokedAt:null,expiresAt:{gt:new Date()}},select:{id:true,deviceId:true,userAgent:true,createdAt:true,lastUsedAt:true,expiresAt:true},orderBy:{lastUsedAt:"desc"}}));
app.delete("/api/v1/account/sessions/:id",{preHandler:authenticate},async(req,reply)=>{const id=z.object({id:z.string().uuid()}).parse(req.params).id;await prisma.refreshSession.updateMany({where:{id,userId:(req as AuthRequest).auth.sub},data:{revokedAt:new Date()}});return reply.code(204).send()});
app.delete("/api/v1/account/sessions",{preHandler:authenticate},async(req,reply)=>{await prisma.refreshSession.updateMany({where:{userId:(req as AuthRequest).auth.sub,revokedAt:null},data:{revokedAt:new Date()}});return reply.code(204).send()});

app.post("/api/v1/admin/auth/login",{config:{rateLimit:{max:6,timeWindow:"15 minutes"}}},async(req,reply)=>{const body=z.object({email:z.string().email(),password:z.string(),deviceId:z.string().min(8)}).parse(req.body);const user=await verifyPasswordAndTrack(prisma,body.email,body.password);if(!user||user.status!=="ACTIVE"||!isAdminRole(user.role))return sendError(reply,req.id,401,"INVALID_CREDENTIALS","Email or password is incorrect");const accessToken=await signAccess(user.id,user.role);const csrf=opaqueToken();reply.setCookie(config.ADMIN_COOKIE_NAME,accessToken,{httpOnly:true,secure:config.NODE_ENV==="production",sameSite:"strict",path:"/",maxAge:config.ACCESS_TOKEN_TTL_SECONDS});reply.setCookie(config.ADMIN_CSRF_COOKIE_NAME,csrf,{httpOnly:false,secure:config.NODE_ENV==="production",sameSite:"strict",path:"/",maxAge:config.ACCESS_TOKEN_TTL_SECONDS});await prisma.adminAuditLog.create({data:{actorUserId:user.id,action:"ADMIN_LOGIN",targetType:"SESSION",requestId:req.id,ipHash:ipHash(req)}});return {user:publicUser(user),csrfToken:csrf}});
app.post("/api/v1/admin/auth/logout",{preHandler:[authenticate,requireCsrf]},async(req,reply)=>{reply.clearCookie(config.ADMIN_COOKIE_NAME,{path:"/"});reply.clearCookie(config.ADMIN_CSRF_COOKIE_NAME,{path:"/"});await audit(req as AuthRequest,"ADMIN_LOGOUT","SESSION");return reply.code(204).send()});
app.get("/api/v1/admin/auth/me",{preHandler:authenticate},async(req,reply)=>{if(!isAdminRole((req as AuthRequest).auth.role))return sendError(reply,req.id,403,"FORBIDDEN","Administrator access required");return publicUser(await prisma.user.findUniqueOrThrow({where:{id:(req as AuthRequest).auth.sub}}))});
app.get("/api/v1/admin/users",{preHandler:requirePermission("users:manage")},async()=>prisma.user.findMany({select:{id:true,email:true,displayName:true,role:true,status:true,emailVerifiedAt:true,createdAt:true},orderBy:{createdAt:"desc"},take:100}));
app.patch("/api/v1/admin/users/:id/status",{preHandler:[requirePermission("users:manage"),requireCsrf]},async(req,reply)=>{const id=z.object({id:z.string().uuid()}).parse(req.params).id;const body=z.object({status:z.enum(["ACTIVE","SUSPENDED","DISABLED"])}).parse(req.body);const user=await prisma.user.update({where:{id},data:{status:body.status}});if(body.status!=="ACTIVE")await prisma.refreshSession.updateMany({where:{userId:id,revokedAt:null},data:{revokedAt:new Date()}});await audit(req as AuthRequest,"USER_STATUS_CHANGED","USER",id,{status:body.status});return publicUser(user)});
app.get("/api/v1/admin/audit-logs",{preHandler:requirePermission("audit:read")},async()=>prisma.adminAuditLog.findMany({orderBy:{createdAt:"desc"},take:100}));

app.get("/api/v1/catalog",{preHandler:authenticate},async()=>({rails:await prisma.collection.findMany({where:{published:true},orderBy:{sortOrder:"asc"},include:{items:{orderBy:{sortOrder:"asc"},include:{movie:true,series:true}}}})}));
app.get("/api/v1/search",{preHandler:authenticate},async(req)=>{const q=z.object({q:z.string().min(2).max(100)}).parse(req.query).q;return prisma.movie.findMany({where:{status:"PUBLISHED",OR:[{title:{contains:q,mode:"insensitive"}},{synopsis:{contains:q,mode:"insensitive"}}]},take:30})});
app.post("/api/v1/playback/sessions",{preHandler:authenticate,config:{rateLimit:{max:20,timeWindow:"1 minute"}}},async(req,reply)=>{const auth=(req as AuthRequest).auth;const body=z.object({videoId:z.string().uuid(),deviceId:z.string().min(8),riskSignals:z.array(z.string()).default([])}).parse(req.body);const user=await prisma.user.findUniqueOrThrow({where:{id:auth.sub}});if(user.status!=="ACTIVE")return sendError(reply,req.id,403,"ACCOUNT_BLOCKED","Playback unavailable");const asset=await prisma.videoAsset.findFirst({where:{id:body.videoId,state:"READY",OR:[{movie:{status:"PUBLISHED"}},{episode:{status:"PUBLISHED"}}]}});if(!asset)return sendError(reply,req.id,404,"VIDEO_UNAVAILABLE","Video is unavailable");const active=await prisma.playbackSession.count({where:{userId:user.id,revokedAt:null,expiresAt:{gt:new Date()}}});if(active>=config.MAX_CONCURRENT_STREAMS)return sendError(reply,req.id,409,"STREAM_LIMIT","Concurrent stream limit reached");const sid=randomUUID(),expiresAt=new Date(Date.now()+config.PLAYBACK_SESSION_TTL_SECONDS*1000);const token=await signPlayback({sub:user.id,sid,videoId:asset.id,deviceId:body.deviceId});await prisma.playbackSession.create({data:{id:sid,userId:user.id,videoAssetId:asset.id,deviceId:body.deviceId,expiresAt,riskSignals:body.riskSignals}});return {sessionId:sid,manifestUrl:`/api/v1/media/${asset.id}/manifest?pt=${token}`,headers:{"X-Playback-Session":sid},watermark:{text:`${user.id.slice(0,4)}..${sid.slice(-4)}`,opacity:.22,moveEverySeconds:18},expiresAt:expiresAt.toISOString()}});

await app.listen({host:"0.0.0.0",port:4000});

import { randomUUID } from "node:crypto";
import argon2 from "argon2";
import type { Prisma, PrismaClient, Role, User } from "@prisma/client";
import { config } from "./config.js";
import { hashToken, opaqueToken, signAccess } from "./security.js";

type Db = PrismaClient | Prisma.TransactionClient;
export type SessionContext = { deviceId: string; userAgent?: string; ipHash?: string };

export const publicUser = (user: Pick<User, "id"|"email"|"displayName"|"role"|"status"|"emailVerifiedAt">) => ({
  id: user.id, email: user.email, displayName: user.displayName, role: user.role,
  status: user.status, emailVerified: Boolean(user.emailVerifiedAt),
});

export async function issueSession(db: Db, user: Pick<User,"id"|"role">, context: SessionContext, family: string = randomUUID()) {
  const refreshToken = opaqueToken();
  const session = await db.refreshSession.create({data:{
    userId:user.id, deviceId:context.deviceId, tokenHash:hashToken(refreshToken), tokenFamily:family,
    userAgent:context.userAgent?.slice(0,300), ipHash:context.ipHash,
    expiresAt:new Date(Date.now()+config.REFRESH_TOKEN_TTL_DAYS*864e5),
  }});
  return { accessToken:await signAccess(user.id,user.role), refreshToken, sessionId:session.id };
}

export async function rotateSession(prisma: PrismaClient, refreshToken: string, context: SessionContext) {
  const tokenHash=hashToken(refreshToken);
  return prisma.$transaction(async tx=>{
    const current=await tx.refreshSession.findUnique({where:{tokenHash},include:{user:true}});
    if(!current) return {kind:"invalid" as const};
    if(current.revokedAt || current.replacedById){
      await tx.refreshSession.updateMany({where:{tokenFamily:current.tokenFamily,revokedAt:null},data:{revokedAt:new Date()}});
      await tx.securityEvent.create({data:{userId:current.userId,kind:"REFRESH_TOKEN_REUSE",severity:"HIGH",metadata:{family:current.tokenFamily}}});
      return {kind:"reuse" as const};
    }
    if(current.expiresAt<=new Date() || current.user.status!=="ACTIVE") return {kind:"expired" as const};
    const next=await issueSession(tx,current.user,context,current.tokenFamily);
    await tx.refreshSession.update({where:{id:current.id},data:{revokedAt:new Date(),replacedById:next.sessionId,lastUsedAt:new Date()}});
    return {kind:"ok" as const,...next,user:publicUser(current.user)};
  });
}

export async function verifyPasswordAndTrack(prisma:PrismaClient,email:string,password:string){
  const user=await prisma.user.findUnique({where:{email:email.toLowerCase()}});
  if(!user) { await argon2.hash(password,{type:argon2.argon2id}); return null; }
  if(user.lockedUntil && user.lockedUntil>new Date()) return null;
  const valid=await argon2.verify(user.passwordHash,password);
  if(!valid){const failures=user.failedLoginCount+1; const lockedUntil=failures>=5?new Date(Date.now()+Math.min(30,2**(failures-5))*60_000):null; await prisma.user.update({where:{id:user.id},data:{failedLoginCount:failures,lockedUntil}}); return null;}
  await prisma.user.update({where:{id:user.id},data:{failedLoginCount:0,lockedUntil:null}});
  return user;
}

export const isAdminRole=(role:Role)=>role==="SUPER_ADMIN"||role==="ADMIN"||role==="EDITOR";

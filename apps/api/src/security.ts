import { createHash, randomBytes } from "node:crypto";
import { SignJWT, jwtVerify } from "jose";
import { config } from "./config.js";
const accessKey=new TextEncoder().encode(config.JWT_ACCESS_SECRET); const playbackKey=new TextEncoder().encode(config.PLAYBACK_SIGNING_SECRET);
export const opaqueToken=()=>randomBytes(32).toString("base64url");
export const hashToken=(token:string)=>createHash("sha256").update(token).digest("hex");
export async function signAccess(sub:string,role:string){return new SignJWT({role}).setProtectedHeader({alg:"HS256"}).setSubject(sub).setIssuedAt().setExpirationTime(`${config.ACCESS_TOKEN_TTL_SECONDS}s`).sign(accessKey)}
export async function verifyAccess(token:string){return (await jwtVerify(token,accessKey,{algorithms:["HS256"]})).payload}
export async function signPlayback(input:{sub:string;sid:string;videoId:string;deviceId:string}){return new SignJWT({sid:input.sid,videoId:input.videoId,deviceId:input.deviceId,scope:"playback"}).setProtectedHeader({alg:"HS256"}).setSubject(input.sub).setIssuedAt().setExpirationTime(`${config.PLAYBACK_SESSION_TTL_SECONDS}s`).sign(playbackKey)}
export async function signOfflineDownload(input:{sub:string;videoId:string;deviceId:string;expiresInSeconds?:number}){return new SignJWT({videoId:input.videoId,deviceId:input.deviceId,scope:"offline-download"}).setProtectedHeader({alg:"HS256"}).setSubject(input.sub).setIssuedAt().setExpirationTime(`${input.expiresInSeconds??604800}s`).sign(playbackKey)}
export async function verifyPlayback(token:string){return (await jwtVerify(token,playbackKey,{algorithms:["HS256"]})).payload}

import assert from "node:assert/strict";
import test from "node:test";
import { hashToken, opaqueToken, signAccess, verifyAccess } from "./security.js";
test("opaque refresh tokens are random and only hashes are stable",()=>{const a=opaqueToken(),b=opaqueToken();assert.notEqual(a,b);assert.equal(hashToken(a),hashToken(a));assert.notEqual(hashToken(a),a)});
test("access tokens carry subject and role",async()=>{const token=await signAccess("viewer-1","VIEWER");const payload=await verifyAccess(token);assert.equal(payload.sub,"viewer-1");assert.equal(payload.role,"VIEWER")});

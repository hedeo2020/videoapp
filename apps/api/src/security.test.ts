import assert from "node:assert/strict";
import test from "node:test";

process.env.DATABASE_URL ??= "postgresql://securestream:securestream@localhost:5432/securestream";
process.env.REDIS_URL ??= "redis://localhost:6379";
process.env.JWT_ACCESS_SECRET ??= "test-access-secret-with-more-than-32-characters";
process.env.JWT_REFRESH_SECRET ??= "test-refresh-secret-with-more-than-32-characters";
process.env.PLAYBACK_SIGNING_SECRET ??= "test-playback-secret-with-more-than-32-characters";
process.env.ADMIN_ORIGIN ??= "http://localhost:3000";

test("opaque refresh tokens are random and only hashes are stable", async () => {
  const { hashToken, opaqueToken } = await import("./security.js");
  const a = opaqueToken(),
    b = opaqueToken();
  assert.notEqual(a, b);
  assert.equal(hashToken(a), hashToken(a));
  assert.notEqual(hashToken(a), a);
});
test("access tokens carry subject and role", async () => {
  const { signAccess, verifyAccess } = await import("./security.js");
  const token = await signAccess("viewer-1", "VIEWER");
  const payload = await verifyAccess(token);
  assert.equal(payload.sub, "viewer-1");
  assert.equal(payload.role, "VIEWER");
});

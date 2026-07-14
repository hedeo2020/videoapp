import assert from "node:assert/strict";
import test from "node:test";

test("blank optional Widevine values are accepted in production development DRM mode", async () => {
  process.env.NODE_ENV = "production";
  process.env.DATABASE_URL = "postgresql://securestream:securestream@localhost:5432/securestream";
  process.env.REDIS_URL = "redis://localhost:6379";
  process.env.JWT_ACCESS_SECRET = "test-access-secret-with-more-than-32-characters";
  process.env.JWT_REFRESH_SECRET = "test-refresh-secret-with-more-than-32-characters";
  process.env.PLAYBACK_SIGNING_SECRET = "test-playback-secret-with-more-than-32-characters";
  process.env.ADMIN_ORIGIN = "https://cpanel.example.com";
  process.env.DRM_PROVIDER = "development";
  process.env.WIDEVINE_LICENSE_URL = "";
  process.env.WIDEVINE_PROVIDER_API_KEY = "";
  const module = await import(`./config.js?blank-widevine=${Date.now()}`);
  assert.equal(module.config.WIDEVINE_LICENSE_URL, undefined);
  assert.equal(module.config.WIDEVINE_PROVIDER_API_KEY, undefined);
});

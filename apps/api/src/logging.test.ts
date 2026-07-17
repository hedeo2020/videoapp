import assert from "node:assert/strict";
import test from "node:test";
import { redactSensitiveQueryParams, requestLogSerializer } from "./logging.js";

test("redacts playback, download, and admin preview tokens while keeping route paths", () => {
  const redacted = redactSensitiveQueryParams(
    "/api/v1/media/asset-1/manifest?pt=secret-playback-token&quality=720&dt=secret-download-token",
  );
  assert.equal(redacted, "/api/v1/media/asset-1/manifest?pt=[redacted]&quality=720&dt=[redacted]");
  assert.match(redacted, /\/api\/v1\/media\/asset-1\/manifest/);
  assert.doesNotMatch(redacted, /secret-playback-token|secret-download-token/);
});

test("request log serializer redacts token query values", () => {
  const serialized = requestLogSerializer({
    method: "GET",
    url: "/api/v1/admin/video-assets/asset-1/preview?token=admin-preview-token",
    hostname: "api.example.com",
  });
  assert.equal(serialized.url, "/api/v1/admin/video-assets/asset-1/preview?token=[redacted]");
  assert.doesNotMatch(JSON.stringify(serialized), /admin-preview-token/);
});

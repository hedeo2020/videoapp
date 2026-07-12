import assert from "node:assert/strict";
import test from "node:test";
import { isCurrentlyAvailable, movieHasReadyAsset } from "./catalog.js";

test("availability windows include currently available catalog only", () => {
  const now = new Date("2026-07-12T12:00:00Z");
  assert.equal(isCurrentlyAvailable({ availabilityStart: new Date("2026-07-01"), availabilityEnd: new Date("2026-08-01") }, now), true);
  assert.equal(isCurrentlyAvailable({ availabilityStart: new Date("2026-08-01"), availabilityEnd: null }, now), false);
  assert.equal(isCurrentlyAvailable({ availabilityStart: null, availabilityEnd: new Date("2026-07-01") }, now), false);
});

test("movie cards require a ready playback asset before surfacing", () => {
  assert.equal(movieHasReadyAsset({ assets: [{ id: "asset-1", state: "READY", durationSeconds: 60 }] } as never), true);
  assert.equal(movieHasReadyAsset({ assets: [{ id: "asset-1", state: "PROCESSING", durationSeconds: 60 }] } as never), false);
});

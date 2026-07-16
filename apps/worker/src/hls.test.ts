import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { buildMasterPlaylist, parseEnabledRenditions, renditionBitrateKbps, renditionKeyFor } from "./hls.js";

describe("HLS helper functions", () => {
  it("parses and sanitizes configured rendition heights", () => {
    assert.deepEqual(parseEnabledRenditions([720, 360, 360, 480]), [360, 480, 720]);
    assert.deepEqual(parseEnabledRenditions("1080, 720, bad, 0, 480"), [480, 720, 1080]);
  });

  it("rejects an empty rendition ladder", () => {
    assert.throws(() => parseEnabledRenditions("bad,0,-1"), /at least one.*rendition/i);
  });

  it("builds safe rendition storage keys", () => {
    assert.equal(renditionKeyFor("asset-123/", 720), "asset-123/720p/playlist.m3u8");
    assert.equal(renditionKeyFor("asset-123", 480), "asset-123/480p/playlist.m3u8");
  });

  it("generates a master playlist that references each variant", () => {
    const playlist = buildMasterPlaylist("asset-123/", [
      { height: 360, bitrateKbps: renditionBitrateKbps(360), codec: "h264", storageKey: "asset-123/360p/playlist.m3u8" },
      { height: 720, bitrateKbps: renditionBitrateKbps(720), codec: "h264", storageKey: "asset-123/720p/playlist.m3u8" },
    ]);

    assert.match(playlist, /^#EXTM3U/);
    assert.match(playlist, /BANDWIDTH=800000/);
    assert.match(playlist, /RESOLUTION=640x360/);
    assert.match(playlist, /360p\/playlist\.m3u8/);
    assert.match(playlist, /BANDWIDTH=2800000/);
    assert.match(playlist, /RESOLUTION=1280x720/);
    assert.match(playlist, /720p\/playlist\.m3u8/);
  });
});

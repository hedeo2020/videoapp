import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { parseCompletedTranscodeResult, shouldMarkJobFailed } from "./processing.js";

describe("processing queue result handling", () => {
  it("accepts a completed transcode result with manifest and renditions", () => {
    const result = parseCompletedTranscodeResult({
      assetId: "96987181-bbbf-42eb-abc1-b2ce5dd9375b",
      manifestKey: "96987181-bbbf-42eb-abc1-b2ce5dd9375b/master.m3u8",
      durationSeconds: 125,
      renditions: [
        {
          height: 720,
          bitrateKbps: 2800,
          codec: "h264",
          storageKey: "96987181-bbbf-42eb-abc1-b2ce5dd9375b/720p/playlist.m3u8",
        },
      ],
    });

    assert.equal(result.assetId, "96987181-bbbf-42eb-abc1-b2ce5dd9375b");
    assert.equal(result.manifestKey, "96987181-bbbf-42eb-abc1-b2ce5dd9375b/master.m3u8");
    assert.equal(result.renditions[0]?.height, 720);
  });

  it("rejects completed results with no renditions", () => {
    assert.throws(
      () =>
        parseCompletedTranscodeResult({
          assetId: "96987181-bbbf-42eb-abc1-b2ce5dd9375b",
          manifestKey: "96987181-bbbf-42eb-abc1-b2ce5dd9375b/master.m3u8",
          durationSeconds: 125,
          renditions: [],
        }),
      /rendition/i,
    );
  });

  it("only marks jobs failed after retries are exhausted", () => {
    assert.equal(shouldMarkJobFailed({ attemptsMade: 1, attempts: 3 }), false);
    assert.equal(shouldMarkJobFailed({ attemptsMade: 3, attempts: 3 }), true);
    assert.equal(shouldMarkJobFailed({ attemptsMade: 1, attempts: undefined }), true);
  });
});

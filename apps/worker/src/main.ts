import { spawn } from "node:child_process";
import { mkdir, statfs, writeFile } from "node:fs/promises";
import path from "node:path";
import { Worker } from "bullmq";
import {
  buildMasterPlaylist,
  normalizedOutputDir,
  parseEnabledRenditions,
  renditionBitrateKbps,
  renditionKeyFor,
  type HlsRendition,
} from "./hls.js";

const connection = { url: process.env.REDIS_URL ?? "redis://redis:6379" };
const concurrency = Number(process.env.WORKER_CONCURRENCY ?? 1);
const minFree = Number(process.env.TEMP_STORAGE_MIN_FREE_BYTES ?? 5368709120);
const temp = process.env.TEMP_PROCESSING_DIR ?? "/data/tmp";
const storageRoot = process.env.STORAGE_LOCAL_ROOT ?? "/data/media";
const ffmpegPreset = process.env.FFMPEG_PRESET?.trim() || "veryfast";
const ffmpegThreads = process.env.FFMPEG_THREADS?.trim() || "2";
const ffmpegAudioBitrate = process.env.FFMPEG_AUDIO_BITRATE?.trim() || "128k";
const ffmpegCrf = process.env.FFMPEG_CRF?.trim();

async function command(bin: string, args: string[]) {
  return await new Promise<string>((resolve, reject) => {
    const p = spawn(bin, args, { stdio: ["ignore", "pipe", "pipe"] });
    let out = "";
    let err = "";
    p.stdout.on("data", (d) => (out += String(d).slice(-8000)));
    p.stderr.on("data", (d) => (err += String(d).slice(-8000)));
    p.on("exit", (c) => (c === 0 ? resolve(out) : reject(new Error(`${bin} exited ${c}: ${err}`))));
  });
}

const storagePathFor = (key: string) => {
  const root = path.resolve(storageRoot);
  const normalizedKey = key.replaceAll("\\", "/").replace(/^\/+/, "");
  const filePath = path.resolve(root, normalizedKey);
  if (!filePath.startsWith(root + path.sep)) throw new Error(`Unsafe storage key rejected: ${key}`);
  return filePath;
};

const parseDurationSeconds = (ffprobeJson: string) => {
  const parsed = JSON.parse(ffprobeJson) as { format?: { duration?: string } };
  const duration = Number(parsed.format?.duration);
  return Number.isFinite(duration) && duration > 0 ? Math.round(duration) : undefined;
};

const worker = new Worker(
  "video-processing",
  async (job) => {
    const fs = await statfs(temp);
    if (fs.bavail * fs.bsize < minFree) throw new Error("Insufficient temporary disk space; processing stopped safely");
    const { assetId, input, outputDir, enabledRenditions } = job.data as {
      assetId: string;
      input: string;
      outputDir: string;
      enabledRenditions?: number[] | string;
    };
    if (!assetId || !input || !outputDir) throw new Error("Invalid transcode job data: assetId, input, and outputDir are required");
    const heights = parseEnabledRenditions(enabledRenditions);
    const safeOutputDir = normalizedOutputDir(outputDir);
    const inputPath = storagePathFor(input);
    await job.updateProgress(5);
    const probe = await command("ffprobe", ["-v", "error", "-show_format", "-show_streams", "-of", "json", inputPath]);
    const durationSeconds = parseDurationSeconds(probe);
    await job.updateProgress(10);
    const renditions: HlsRendition[] = [];
    for (const [index, height] of heights.entries()) {
      const storageKey = renditionKeyFor(safeOutputDir, height);
      const playlistPath = storagePathFor(storageKey);
      const segmentPath = path.join(path.dirname(playlistPath), "segment_%05d.ts");
      await mkdir(path.dirname(playlistPath), { recursive: true });
      const args = [
        "-y",
        "-i",
        inputPath,
        "-threads",
        ffmpegThreads,
        "-vf",
        `scale=-2:${height}`,
        "-c:v",
        "libx264",
        "-preset",
        ffmpegPreset,
        ...(ffmpegCrf ? ["-crf", ffmpegCrf] : []),
        "-c:a",
        "aac",
        "-b:a",
        ffmpegAudioBitrate,
        "-f",
        "hls",
        "-hls_time",
        "6",
        "-hls_playlist_type",
        "vod",
        "-hls_segment_filename",
        segmentPath,
        playlistPath,
      ];
      await command("ffmpeg", args);
      renditions.push({ height, bitrateKbps: renditionBitrateKbps(height), codec: "h264", storageKey });
      await job.updateProgress(Math.min(95, 10 + Math.round(((index + 1) / heights.length) * 85)));
    }
    const manifestKey = `${safeOutputDir}master.m3u8`;
    await writeFile(storagePathFor(manifestKey), buildMasterPlaylist(safeOutputDir, renditions), "utf8");
    await job.updateProgress(100);
    return { assetId, manifestKey, durationSeconds, renditions };
  },
  { connection, concurrency },
);
worker.on("completed", (job) => console.log(JSON.stringify({ level: "info", event: "job.completed", jobId: job.id })));
worker.on("failed", (job, error) =>
  console.error(JSON.stringify({ level: "error", event: "job.failed", jobId: job?.id, message: error.message })),
);
console.log(JSON.stringify({ level: "info", event: "worker.started", concurrency }));

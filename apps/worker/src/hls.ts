export type HlsRendition = {
  height: number;
  bitrateKbps: number;
  codec: "h264";
  storageKey: string;
};

const maxReasonableHeight = 4320;

export const parseEnabledRenditions = (value: unknown): number[] => {
  const rawValues = Array.isArray(value) ? value : typeof value === "string" ? value.split(",") : [];
  const heights = rawValues
    .map((raw) => Number(raw))
    .filter((height) => Number.isInteger(height) && height > 0 && height <= maxReasonableHeight);
  const unique = [...new Set(heights)].sort((a, b) => a - b);
  if (unique.length === 0) throw new Error("At least one enabled HLS rendition height is required");
  return unique;
};

export const renditionBitrateKbps = (height: number) => {
  if (height <= 240) return 400;
  if (height <= 360) return 800;
  if (height <= 480) return 1400;
  if (height <= 720) return 2800;
  if (height <= 1080) return 5000;
  if (height <= 1440) return 9000;
  return 16000;
};

export const normalizedOutputDir = (outputDir: string) => {
  const trimmed = outputDir.trim().replaceAll("\\", "/").replace(/^\/+/, "");
  return trimmed.endsWith("/") ? trimmed : `${trimmed}/`;
};

export const renditionKeyFor = (outputDir: string, height: number) =>
  `${normalizedOutputDir(outputDir)}${height}p/playlist.m3u8`;

const relativePlaylistPath = (outputDir: string, storageKey: string) => {
  const base = normalizedOutputDir(outputDir);
  return storageKey.startsWith(base) ? storageKey.slice(base.length) : storageKey;
};

export const resolutionForHeight = (height: number) => {
  const width = Math.max(2, Math.round((height * 16) / 9 / 2) * 2);
  return `${width}x${height}`;
};

export const buildMasterPlaylist = (outputDir: string, renditions: HlsRendition[]) => {
  if (renditions.length === 0) throw new Error("Cannot build a master playlist without renditions");
  const lines = ["#EXTM3U", "#EXT-X-VERSION:3"];
  for (const rendition of [...renditions].sort((a, b) => a.height - b.height)) {
    lines.push(
      `#EXT-X-STREAM-INF:BANDWIDTH=${rendition.bitrateKbps * 1000},RESOLUTION=${resolutionForHeight(
        rendition.height,
      )},CODECS="avc1.42e01e,mp4a.40.2"`,
      relativePlaylistPath(outputDir, rendition.storageKey),
    );
  }
  return `${lines.join("\n")}\n`;
};

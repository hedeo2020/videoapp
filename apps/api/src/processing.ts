import { z } from "zod";

const renditionSchema = z.object({
  height: z.number().int().positive().max(4320),
  bitrateKbps: z.number().int().positive(),
  codec: z.string().min(1).max(32),
  storageKey: z.string().min(1),
});

const completedTranscodeSchema = z.object({
  assetId: z.string().uuid(),
  manifestKey: z.string().min(1),
  durationSeconds: z.number().int().positive().optional(),
  renditions: z.array(renditionSchema).min(1),
});

export type CompletedTranscodeResult = z.infer<typeof completedTranscodeSchema>;

export const parseCompletedTranscodeResult = (value: unknown): CompletedTranscodeResult => {
  const parsed = typeof value === "string" ? JSON.parse(value) : value;
  return completedTranscodeSchema.parse(parsed);
};

export const shouldMarkJobFailed = ({
  attemptsMade,
  attempts,
}: {
  attemptsMade: number;
  attempts: number | undefined;
}) => attemptsMade >= (attempts ?? 1);

import { z } from "zod";

const optionalString = z.preprocess(
  (v) => (typeof v === "string" && v.trim() === "" ? undefined : v),
  z.string().optional(),
);
const optionalUrl = z.preprocess(
  (v) => (typeof v === "string" && v.trim() === "" ? undefined : v),
  z.string().url().optional(),
);
const renditionList = z.preprocess(
  (value) => (typeof value === "string" ? value.split(",").map((part) => Number(part.trim())) : value),
  z
    .array(z.number().int().positive().max(4320))
    .min(1)
    .transform((heights) => [...new Set(heights)].sort((a, b) => a - b)),
);
const adminOriginList = z.preprocess(
  (value) =>
    typeof value === "string"
      ? value
          .split(",")
          .map((part) => part.trim())
          .filter(Boolean)
      : value,
  z.array(z.string().url()).min(1),
);
export const config = z
  .object({
    NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
    DATABASE_URL: z.string().min(1),
    REDIS_URL: z.string().min(1),
    JWT_ACCESS_SECRET: z.string().min(32),
    JWT_REFRESH_SECRET: z.string().min(32),
    PLAYBACK_SIGNING_SECRET: z.string().min(32),
    ADMIN_ORIGIN: adminOriginList,
    REGISTRATION_ENABLED: z.coerce.boolean().default(true),
    ACCESS_TOKEN_TTL_SECONDS: z.coerce.number().int().min(60).default(86400),
    REFRESH_TOKEN_TTL_DAYS: z.coerce.number().int().min(1).max(90).default(30),
    PLAYBACK_SESSION_TTL_SECONDS: z.coerce.number().int().min(30).max(86400).default(3600),
    MAX_CONCURRENT_STREAMS: z.coerce.number().int().min(1).default(2),
    DRM_PROVIDER: z.enum(["development", "widevine"]).default("development"),
    WIDEVINE_LICENSE_URL: optionalUrl,
    WIDEVINE_PROVIDER_API_KEY: optionalString,
    EMAIL_TOKEN_TTL_MINUTES: z.coerce.number().int().min(5).default(30),
    ADMIN_COOKIE_NAME: z.string().default("ss_admin"),
    ADMIN_CSRF_COOKIE_NAME: z.string().default("ss_csrf"),
    ENABLED_RENDITIONS: renditionList.default([360, 480, 720]),
  })
  .superRefine((v, c) => {
    if (
      v.NODE_ENV === "production" &&
      v.DRM_PROVIDER === "widevine" &&
      (!v.WIDEVINE_LICENSE_URL || !v.WIDEVINE_PROVIDER_API_KEY)
    )
      c.addIssue({ code: "custom", message: "Widevine provider configuration is required in production" });
  })
  .parse(process.env);

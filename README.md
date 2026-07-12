# SecureStream

A production-oriented, original video streaming foundation: native Android viewer, security-focused API, administrator operations console, FFmpeg worker, PostgreSQL, Redis, and ARM64 Docker deployment.

## Quick start

1. Copy `.env.example` to `.env` and replace every secret.
2. Run `pnpm install`.
3. Run `docker compose up -d postgres redis`.
4. Run `pnpm --filter @securestream/api prisma:dev` then `pnpm dev`.
5. Open the admin console at `http://localhost:3000`; the API is at `http://localhost:4000`.

The Android app uses `http://10.0.2.2:4000` from an emulator. Build it on a developer machine or CI, never on the production VPS.

## Security stance

Original assets are private, playback grants are short-lived and scoped, access is server-authorized, sensitive logs are redacted, and protected Android windows use `FLAG_SECURE`. Production protected titles require a legitimate Widevine DRM provider. Development HLS is non-DRM and is not a substitute. Rooted devices and external cameras can still capture video; see [the threat model](docs/THREAT_MODEL.md).

Architecture and milestones are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). OCI/Coolify operations are in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

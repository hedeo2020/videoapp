# SecureStream

A production-oriented, original video streaming foundation: native Android viewer, security-focused API, administrator operations console, FFmpeg worker, PostgreSQL, Redis, and ARM64 Docker deployment.

## Quick start

1. Copy `.env.example` to `.env` and replace every secret.
2. Run `pnpm install`.
3. Run `docker compose up -d postgres redis`.
4. Run `pnpm --filter @securestream/api prisma:dev` then `pnpm dev`.
5. Open the admin console at `http://localhost:3000`; the API is at `http://localhost:4000`.

Create the first administrator after applying migrations:

```bash
pnpm --filter @securestream/api admin:create
```

For automated initial deployment, provide `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_NAME`, and `BOOTSTRAP_ADMIN_PASSWORD`, run the same command once, then remove those secrets and rotate the credential.

The Android app uses `http://10.0.2.2:4000` from an emulator. Build it on a developer machine or CI, never on the production VPS.

## Security stance

Original assets are private, playback grants are short-lived and scoped, access is server-authorized, sensitive logs are redacted, and protected Android windows use `FLAG_SECURE`. Production protected titles require a legitimate Widevine DRM provider. Development HLS is non-DRM and is not a substitute. Rooted devices and external cameras can still capture video; see [the threat model](docs/THREAT_MODEL.md).

Architecture and milestones are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). OCI/Coolify operations are in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md). The complete portable install/migration guide is in [docs/PORTABLE_PACKAGE.md](docs/PORTABLE_PACKAGE.md), and the step-by-step Coolify guide is in [docs/COOLIFY_INSTALLATION.md](docs/COOLIFY_INSTALLATION.md).

## Identity API

Viewer identity includes registration, login, rotating refresh tokens with family-wide reuse revocation, logout, generic password recovery, email verification, current-user lookup, active-session listing, and individual/all-session revocation. Administrator identity uses a separate login route, short-lived HTTP-only cookies, strict same-site policy, CSRF validation for mutations, server-enforced role permissions, and audit events.

## Catalog and playback API

Administrators can create and update movies, series, seasons, episodes, collections, video assets, multipart uploads, and processing jobs through `/api/v1/admin/*`. Publishing a movie is blocked until at least one ready video asset exists. Viewer catalog rails only return published, available titles with ready media, and search filters out expired or unplayable results.

Playback sessions enforce active account status, availability windows, ready manifests, stream limits, scoped short-lived playback tokens, server-approved watermark text, watch history, and progress sync. Production Widevine mode fails closed when a protected asset lacks provider configuration or a key ID.

## Validation

Run `pnpm build` before deployment and `pnpm --filter @securestream/api test` for the security/catalog rule suite. CI also validates the Docker Compose configuration and ARM64 images.

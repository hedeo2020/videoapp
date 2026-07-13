# Coolify deployment on OCI Ampere A1

Use Ubuntu ARM64, keep ports 5432 and 6379 private, and expose only the API and admin services through Coolify's HTTPS proxy. Import the repository as a Docker Compose resource and combine `docker-compose.yml` with `docker-compose.production.yml`. Configure `api.example.com` for port 4000 and `admin.example.com` for port 3000. Put every secret from `.env.example` in Coolify; generate independent 32+ byte random secrets. Use private S3-compatible storage in production and do not deploy MinIO on the smallest host.

If Coolify is configured as a single Dockerfile resource, the root `Dockerfile` builds only the API container on port 4000. That mode still requires external PostgreSQL and Redis URLs and does not run the admin console or worker. Use Docker Compose mode for the complete platform.

To deploy the admin console as its own Coolify Dockerfile resource, keep the base directory as `/`, set the Dockerfile location to `apps/admin/Dockerfile`, expose port `3000`, and set `NEXT_PUBLIC_API_URL` to the public API URL ending in `/api/v1`. Because Next.js embeds public variables at build time, redeploy the admin app after changing `NEXT_PUBLIC_API_URL`.

For one-day idle login sessions, set `ACCESS_TOKEN_TTL_SECONDS=86400` on the API service in Coolify. This controls viewer access tokens plus the admin secure cookie lifetime. Keep `REFRESH_TOKEN_TTL_DAYS=30` or higher for mobile refresh sessions. If you want playback grants to last longer while a user watches, set `PLAYBACK_SESSION_TTL_SECONDS=3600` or another value up to `86400`.

Preview and editor MP4 outputs use storage-friendly H.264 settings by default: `FFMPEG_PRESET=slow`, `FFMPEG_CRF=24`, and `FFMPEG_AUDIO_BITRATE=96k`. Lower CRF means higher quality and larger files; higher CRF means smaller files. For near-source quality, use `FFMPEG_CRF=20` or `22`. For tighter storage savings, use `26` or `28`.

Run database migrations before switching traffic. Create the first administrator with `pnpm --filter @securestream/api admin:create`; never retain bootstrap credentials in the environment. Back up PostgreSQL with `pg_dump --format=custom`, back up object storage using provider versioning/replication, and test `pg_restore` in an isolated database. Roll back by restoring the prior image tags and applying only a migration explicitly documented as reversible.

A small Ampere VPS is appropriate for development and limited early use, not Netflix-scale traffic or large-scale concurrent transcoding. Keep worker concurrency at one and move storage, processing, Redis, and PostgreSQL to dedicated services as usage grows.

## Launch checklist

Before launch, confirm `pnpm build`, `pnpm --filter @securestream/api test`, database migrations, and Docker Compose validation all pass. Verify `/health` and `/ready` behind HTTPS, create a non-bootstrap administrator, publish one test movie with a ready asset, and complete a full viewer login, catalog, playback-session, progress, history, and logout flow.

Enable PostgreSQL backups before importing real users. Use provider-side object storage versioning or replication for source media and packaged manifests. Keep Redis disposable: sessions can be reissued, queues can be replayed from upload state, and PostgreSQL remains authoritative.

For production DRM, set `DRM_PROVIDER=widevine`, provide `WIDEVINE_LICENSE_URL` and `WIDEVINE_PROVIDER_API_KEY`, and ensure every protected asset has a provider key ID before publishing. The API intentionally denies playback when Widevine is selected but the asset/provider configuration is incomplete.

# SecureStream portable installation package

This repository is the complete SecureStream package:

- API server: authentication, catalog, uploads, playback, offline downloads, admin automation, backups, alerts, and system panels.
- Admin web panel: upload/publish, users, folders, chat, notifications, backups, settings, maintenance mode, storage, trash, and device sessions.
- Android viewer project: Kotlin/Compose viewer app source and AI Studio implementation notes.
- Worker: FFmpeg/background processing service.
- Database/queue: PostgreSQL and Redis through Docker Compose.

The project is portable. You can move it to another server, domain, or port as long as you update the environment URLs and keep `/data/media` persistent.

## Project structure

```text
videoapp/
  apps/
    api/                 SecureStream API, Prisma schema, migrations, admin scripts
    admin/               Next.js admin panel
    android/             Android viewer project and AI Studio notes
    worker/              Background worker for video processing
  docs/
    PORTABLE_PACKAGE.md  This full install/migration guide
    DEPLOYMENT.md        OCI/Coolify notes
    ANDROID.md           Android runtime contract
    THREAT_MODEL.md      Security expectations
  packages/
    shared-types/        Shared TypeScript types
  docker-compose.yml
  docker-compose.production.yml
  .env.example
  .env.production.example
```

## Server requirements

Recommended minimum:

- Ubuntu 22.04/24.04 ARM64 or AMD64
- Docker + Docker Compose plugin
- 3 OCPU / 12 GB RAM is okay for early use
- A persistent disk/volume mounted to `/data/media`
- HTTPS reverse proxy or a platform like Coolify

Do not expose PostgreSQL or Redis publicly.

## One-server Docker Compose installation

1. Clone the repo:

```bash
git clone https://github.com/hedeo2020/videoapp.git
cd videoapp
```

2. Create env file:

```bash
cp .env.production.example .env
```

3. Edit `.env`.

Required values:

```env
DATABASE_URL=postgresql://securestream:YOUR_DB_PASSWORD@postgres:5432/securestream
REDIS_URL=redis://redis:6379
JWT_ACCESS_SECRET=32-plus-random-characters
JWT_REFRESH_SECRET=another-32-plus-random-characters
PLAYBACK_SIGNING_SECRET=another-32-plus-random-characters
ADMIN_ORIGIN=https://your-admin-domain.com
API_PUBLIC_URL=https://your-api-domain.com
NEXT_PUBLIC_API_URL=https://your-api-domain.com/api/v1
STORAGE_LOCAL_ROOT=/data/media
```

Important: `NEXT_PUBLIC_API_URL` is embedded into the admin build. If you change API domain later, rebuild/redeploy the admin service.

4. Start the full package:

```bash
docker compose up -d --build
```

5. Check services:

```bash
docker compose ps
docker compose logs -f api
```

6. Create first admin:

```bash
docker compose exec api pnpm --filter @securestream/api admin:create
```

If using bootstrap env variables, set these temporarily:

```env
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_NAME=Admin
BOOTSTRAP_ADMIN_PASSWORD=change-this-strong-password
```

Then run the same admin command once and remove those bootstrap variables.

## Coolify installation

Use Docker Compose mode for the full package.

Recommended service domains:

- API: `https://api.yourdomain.com` → internal port `4000`
- Admin: `https://admin.yourdomain.com` → internal port `3000`

In Coolify:

1. Import this GitHub repo.
2. Use `docker-compose.yml`.
3. Add `docker-compose.production.yml` as the production override if supported.
4. Add every variable from `.env.production.example`.
5. Set persistent storage for the API and worker at:

```text
/data/media
```

6. Redeploy after every URL/env change.

If deploying admin as a separate Dockerfile resource:

- Base directory: `/`
- Dockerfile: `apps/admin/Dockerfile`
- Port: `3000`
- Build/runtime env: `NEXT_PUBLIC_API_URL=https://api.yourdomain.com/api/v1`

## Changing to a different server, URL, or port

When moving servers, update only these values:

```env
ADMIN_ORIGIN=https://new-admin-domain.com
API_PUBLIC_URL=https://new-api-domain.com
NEXT_PUBLIC_API_URL=https://new-api-domain.com/api/v1
DATABASE_URL=postgresql://...
REDIS_URL=redis://...
```

Then rebuild/redeploy:

```bash
docker compose up -d --build
```

Why this works:

- Media is stored by relative keys under `/data/media`.
- Backups store media by relative path, not by old domain.
- Playback/download URLs are generated from the current request host/proxy.
- Admin uses `NEXT_PUBLIC_API_URL`, so it must be rebuilt when the API URL changes.

## Persistent storage rules

The API and worker must share the same persistent media path:

```text
/data/media
```

This stores:

- uploaded source files
- MP4 previews
- thumbnails
- edited clips
- portable backups

If `/data/media` is not persistent, uploads may disappear after redeploy.

## Database migrations

The API Dockerfile automatically runs:

```bash
pnpm --filter @securestream/api prisma:migrate
```

If you need to run migrations manually:

```bash
docker compose exec api pnpm --filter @securestream/api prisma:migrate
```

## Backup and restore

Admin panel:

```text
Backup & Restore → Create downloadable backup
```

The backup includes:

- users
- catalog/folders
- video access rules
- chat/messages
- notifications
- watch data
- media files from `/data/media`

Restore:

```text
Backup & Restore → Restore backup
```

The restore process validates that the file is a real SecureStream backup before replacing data.

Optional Google Drive backup upload env:

```env
GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON=
GOOGLE_DRIVE_FOLDER_ID=
```

## Alerts

Set any of these on the API server:

```env
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...
ALERT_WEBHOOK_URL=https://your-webhook-url
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

Test from:

```text
Admin Panel → Settings → Test alert
```

Alerts are used for backup/test/scheduled backup events.

## Maintenance mode

Admin panel:

```text
Settings → Maintenance mode → Save settings
```

When enabled:

- viewer login/API calls are paused
- admins can still login
- Android can read maintenance status from:

```text
GET /api/v1/app/config
```

## Android app handoff

The Android app is viewer-only. Do not build admin upload features into it.

For Google AI Studio, use:

```text
API base URL:
https://your-api-domain.com/api/v1
```

Required Android behavior:

- login with `/auth/login`
- store tokens securely
- catalog from `/catalog`
- search from `/search?q=`
- playback via `POST /playback/sessions`
- use the returned `manifestUrl` exactly as returned
- do not prepend the API base URL to `manifestUrl`
- send progress to `/playback/progress`
- offline download via `/offline/downloads`
- keep downloaded videos in app-private storage only
- read maintenance/update status from `/app/config`

Android project folder:

```text
apps/android/
```

Extra Android notes:

```text
apps/android/OFFLINE_DOWNLOADS.md
docs/ANDROID.md
```

## Admin panel first checks after deploy

After deploy, login to admin and check:

1. Overview: system status is updating.
2. Settings: API URLs, maintenance mode, alert test.
3. Uploads: upload one small MP4 and publish it.
4. Catalog: confirm folder/video arrangement.
5. Users: create a viewer and assign folder/video access.
6. Android: login viewer, catalog appears, playback works.
7. Backup & Restore: create one backup.
8. Storage: confirm `/data/media` usage is visible.

## Troubleshooting

### Admin login stuck or API unreachable

Check:

```env
ADMIN_ORIGIN=https://your-admin-domain.com
NEXT_PUBLIC_API_URL=https://your-api-domain.com/api/v1
```

Redeploy admin after changing `NEXT_PUBLIC_API_URL`.

### Upload works but video disappears after redeploy

Your API does not have persistent storage mounted at:

```text
/data/media
```

Mount persistent storage for API and worker.

### Android says HTTP 404/409 during playback

Check:

- the video is published
- video asset is ready
- user has access to the folder/video
- API and Android use the same public API URL
- Android uses `manifestUrl` exactly as returned

### User deleted but Android still has old token

The API rejects deleted users and revoked sessions. The Android app should logout locally when it receives:

```text
401 ACCOUNT_INACTIVE
403 ACCOUNT_BLOCKED
```

### Discord alert does not arrive

Check:

```env
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...
```

Then redeploy API and click:

```text
Settings → Test alert
```

## Final deployment checklist

- `.env` uses production URLs.
- `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET`, and `PLAYBACK_SIGNING_SECRET` are unique.
- API and worker share persistent `/data/media`.
- PostgreSQL and Redis are private.
- Admin and API are behind HTTPS.
- `docker compose up -d --build` succeeds.
- `GET /health` and `GET /ready` succeed.
- First admin account exists.
- Test viewer account can login.
- Test video can upload, publish, play online, and download offline.
- Backup file can be created and downloaded.

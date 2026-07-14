# SecureStream Coolify separate resources guide

Use this guide when you want Coolify to look like your reference screenshot:

```text
Applications
- SecureStream API
- SecureStream Admin

Databases
- PostgreSQL
- Redis
```

This is not a one-file Docker Compose deployment. Coolify only shows separate database cards when you create the databases using Coolify's own database resource buttons.

## Final Coolify shape

Create exactly these four resources:

```text
Application: securestream-api
Application: securestream-admin
Database:    securestream-postgres
Database:    securestream-redis
```

The background video worker runs inside the API application, so you do not need a third worker application card.

## Required application ports

Use these exact internal application ports in Coolify:

```text
API application   -> port 4000
Admin application -> port 3000
```

Do not set both applications to port `3000`.

If the API application is set to `3000`, Coolify's proxy will point to the wrong port and the API domain can show `Bad Gateway`.

## Step 1: Delete the old Compose application

If you currently have one big Compose resource, delete it.

If this is a fresh install and you do not need the old data, also delete the old Compose volumes.

Do not use these files for the separate-card setup:

```text
docker-compose.yml
docker-compose.coolify.yml
docker-compose.production.yml
```

Those files are only for Compose-style deployments.

## Step 2: Create PostgreSQL database

In Coolify:

1. Click `+ New`.
2. Choose `Database`.
3. Choose `PostgreSQL`.
4. Name it:

```text
securestream-postgres
```

Recommended values:

```text
Database: securestream
Username: securestream
Password: generate a strong password
```

After it starts, copy the internal/private PostgreSQL connection string from Coolify.

It should look similar to:

```text
postgresql://securestream:PASSWORD@POSTGRES_INTERNAL_HOST:5432/securestream
```

Use Coolify's internal/private URL, not a public URL.

## Step 3: Create Redis database

In Coolify:

1. Click `+ New`.
2. Choose `Database`.
3. Choose `Redis`.
4. Name it:

```text
securestream-redis
```

After it starts, copy the internal/private Redis connection string from Coolify.

It should look similar to:

```text
redis://default:PASSWORD@REDIS_INTERNAL_HOST:6379
```

or:

```text
redis://REDIS_INTERNAL_HOST:6379
```

Use Coolify's internal/private URL.

## Step 4: Create the API application

In Coolify:

1. Click `+ New`.
2. Choose `Application`.
3. Choose GitHub repository:

```text
https://github.com/hedeo2020/videoapp.git
```

4. Branch:

```text
main
```

5. Build pack/type:

```text
Dockerfile
```

6. Base directory:

```text
/
```

7. Dockerfile location:

```text
Dockerfile
```

8. Port:

```text
4000
```

9. Domain:

```text
https://api.yourdomain.com
```

Example:

```text
https://api.3dbpoint.com
```

## Step 5: API environment variables

Add these to the API application environment variables.

Use your real Coolify database URLs from Step 2 and Step 3.

This block includes every variable from `.env.production.example`.

```env
NODE_ENV=production
PRODUCT_NAME=SecureStream

# Database and queue
POSTGRES_USER=securestream
POSTGRES_PASSWORD=YOUR_POSTGRES_PASSWORD
POSTGRES_DB=securestream
DATABASE_URL=postgresql://securestream:YOUR_POSTGRES_PASSWORD@YOUR_POSTGRES_INTERNAL_HOST:5432/securestream
REDIS_URL=redis://YOUR_REDIS_INTERNAL_HOST:6379

# Required independent 32+ character secrets. Do not reuse one value for all three.
JWT_ACCESS_SECRET=CHANGE_ME_32_PLUS_RANDOM_CHARACTERS_ACCESS
JWT_REFRESH_SECRET=CHANGE_ME_32_PLUS_RANDOM_CHARACTERS_REFRESH
PLAYBACK_SIGNING_SECRET=CHANGE_ME_32_PLUS_RANDOM_CHARACTERS_PLAYBACK

# Login/session behavior
ACCESS_TOKEN_TTL_SECONDS=86400
REFRESH_TOKEN_TTL_DAYS=30
EMAIL_TOKEN_TTL_MINUTES=30
ADMIN_COOKIE_NAME=ss_admin
ADMIN_CSRF_COOKIE_NAME=ss_csrf
PLAYBACK_SESSION_TTL_SECONDS=3600
REGISTRATION_ENABLED=true
MAX_CONCURRENT_STREAMS=2

# Public URLs for the server where you deploy.
# Change these when moving to a new domain/port.
ADMIN_ORIGIN=https://admin.yourdomain.com
API_PUBLIC_URL=https://api.yourdomain.com
NEXT_PUBLIC_API_URL=https://api.yourdomain.com/api/v1

# Persistent media storage. Mount this path as a persistent volume.
STORAGE_DRIVER=local
STORAGE_LOCAL_ROOT=/data/media

# Upload/conversion behavior
WORKER_CONCURRENCY=1
FFMPEG_THREADS=2
FFMPEG_PRESET=veryfast
FFMPEG_CRF=
FFMPEG_AUDIO_BITRATE=128k
ENABLED_RENDITIONS=360,480,720
MAX_UPLOAD_BYTES=10737418240
MAX_UPLOAD_FILES=20
TEMP_PROCESSING_DIR=/data/tmp
TEMP_STORAGE_MIN_FREE_BYTES=5368709120

# Optional S3-compatible storage placeholders. Current deployment uses local persistent storage.
S3_ENDPOINT=
S3_BUCKET=
S3_REGION=
S3_ACCESS_KEY_ID=
S3_SECRET_ACCESS_KEY=

# DRM. Keep development unless you have a real Widevine provider.
DRM_PROVIDER=development
WIDEVINE_LICENSE_URL=
WIDEVINE_PROVIDER_API_KEY=

# Optional backup upload to Google Drive.
# GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON can be raw JSON or base64 JSON.
GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON=
GOOGLE_DRIVE_FOLDER_ID=

# Optional alerts. Any configured target receives backup/test/system alerts.
DISCORD_WEBHOOK_URL=
ALERT_WEBHOOK_URL=
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=

# Optional one-time bootstrap admin creation.
# Remove after running: pnpm --filter @securestream/api admin:create
BOOTSTRAP_ADMIN_EMAIL=
BOOTSTRAP_ADMIN_NAME=
BOOTSTRAP_ADMIN_PASSWORD=
```

For your current domains, use:

```env
ADMIN_ORIGIN=https://cpanel.3dbpoint.com
API_PUBLIC_URL=https://api.3dbpoint.com
NEXT_PUBLIC_API_URL=https://api.3dbpoint.com/api/v1
```

## Step 6: API persistent storage

In the API application, add persistent storage:

```text
Mount path: /data
```

This stores:

- uploaded source videos
- MP4 previews
- thumbnails
- edited clips
- backup files

If `/data` is not persistent, videos can disappear after redeploy.

## Step 7: Create the Admin application

In Coolify:

1. Click `+ New`.
2. Choose `Application`.
3. Choose the same GitHub repository:

```text
https://github.com/hedeo2020/videoapp.git
```

4. Branch:

```text
main
```

5. Build pack/type:

```text
Dockerfile
```

6. Base directory:

```text
/
```

7. Dockerfile location:

```text
apps/admin/Dockerfile
```

8. Port:

```text
3000
```

9. Domain:

```text
https://admin.yourdomain.com
```

Example:

```text
https://cpanel.3dbpoint.com
```

## Step 8: Admin environment variables

Add this to the Admin application environment variables:

```env
NODE_ENV=production
NEXT_PUBLIC_API_URL=https://api.yourdomain.com/api/v1
```

For your current domain:

```env
NODE_ENV=production
NEXT_PUBLIC_API_URL=https://api.3dbpoint.com/api/v1
```

Important: `NEXT_PUBLIC_API_URL` is baked into the admin build. If you change it, rebuild the admin application.

## Step 9: Deploy order

Deploy in this order:

1. PostgreSQL
2. Redis
3. API
4. Admin

## Step 10: Create the first admin user

Open the terminal for the API application and run:

```bash
pnpm --filter @securestream/api admin:create
```

If the API terminal starts in `/app`, this command should work directly.

## What the Coolify page should show

After this setup, your Coolify environment should look like:

```text
Applications
- securestream-api
- securestream-admin

Databases
- securestream-postgres
- securestream-redis
```

That is the same style as your reference screenshot.

## Troubleshooting

### I still see only one application card

You created a Docker Compose resource again.

Delete it and create resources one by one:

```text
+ New -> Application
+ New -> Database
```

Do not choose Docker Compose for this setup.

### Prisma P1000 / database authentication failed

The password inside `DATABASE_URL` does not match the PostgreSQL database resource.

Copy the internal connection string again from the Coolify PostgreSQL resource.

### Redis self-signed certificate error

Use the internal Redis URL from the Coolify Redis resource. Prefer `redis://...` internally unless your Coolify Redis resource specifically requires `rediss://...`.

### Admin cannot login

Check:

```env
ADMIN_ORIGIN=https://cpanel.3dbpoint.com
NEXT_PUBLIC_API_URL=https://api.3dbpoint.com/api/v1
```

Then rebuild both API and Admin.

### API domain shows Bad Gateway

First check the API application's port in Coolify.

It must be:

```text
4000
```

The Admin application uses:

```text
3000
```

If both API and Admin are set to `3000`, fix the API application port to `4000`, save, then redeploy/restart the API application.

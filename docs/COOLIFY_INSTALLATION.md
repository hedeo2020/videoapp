# SecureStream Coolify installation guide

This guide is for installing SecureStream on a VPS using Coolify.

Use this when you want the full platform running in Coolify:

- API
- Admin panel
- Worker
- PostgreSQL
- Redis
- Persistent media storage

## Recommended Coolify setup

Use Coolify's Docker Compose app/resource, not a single Dockerfile app.

Why:

- The full platform needs multiple services.
- PostgreSQL and Redis are included in `docker-compose.yml`.
- The API, admin, and worker must share the same environment.
- The API and worker must share persistent media storage.

## Domains

Prepare two domains or subdomains:

```text
api.yourdomain.com
admin.yourdomain.com
```

Recommended mapping:

```text
API service   -> port 4000 -> https://api.yourdomain.com
Admin service -> port 3000 -> https://admin.yourdomain.com
```

Do not expose these publicly:

```text
postgres -> 5432
redis    -> 6379
worker   -> no public port
```

## Step 1: Create a new Coolify resource

In Coolify:

1. Go to your Project.
2. Click New Resource.
3. Choose Docker Compose.
4. Connect the GitHub repository:

```text
https://github.com/hedeo2020/videoapp.git
```

5. Select branch:

```text
main
```

6. Compose file:

```text
docker-compose.yml
```

If Coolify allows a production override file, also add:

```text
docker-compose.production.yml
```

## Step 2: Add environment variables

Copy the variables from:

```text
.env.production.example
```

Add them in Coolify's Environment Variables area.

Minimum required values:

```env
NODE_ENV=production
POSTGRES_USER=securestream
POSTGRES_PASSWORD=YOUR_DB_PASSWORD
POSTGRES_DB=securestream
DATABASE_URL=postgresql://securestream:YOUR_DB_PASSWORD@postgres:5432/securestream
REDIS_URL=redis://redis:6379
JWT_ACCESS_SECRET=CHANGE_THIS_TO_32_PLUS_RANDOM_CHARS
JWT_REFRESH_SECRET=CHANGE_THIS_TO_ANOTHER_32_PLUS_RANDOM_CHARS
PLAYBACK_SIGNING_SECRET=CHANGE_THIS_TO_ANOTHER_32_PLUS_RANDOM_CHARS
ADMIN_ORIGIN=https://admin.yourdomain.com
API_PUBLIC_URL=https://api.yourdomain.com
NEXT_PUBLIC_API_URL=https://api.yourdomain.com/api/v1
STORAGE_LOCAL_ROOT=/data/media
ACCESS_TOKEN_TTL_SECONDS=86400
REFRESH_TOKEN_TTL_DAYS=30
PLAYBACK_SESSION_TTL_SECONDS=3600
REGISTRATION_ENABLED=true
MAX_CONCURRENT_STREAMS=2
DRM_PROVIDER=development
WORKER_CONCURRENCY=1
MAX_UPLOAD_BYTES=10737418240
MAX_UPLOAD_FILES=20
TEMP_PROCESSING_DIR=/data/tmp
TEMP_STORAGE_MIN_FREE_BYTES=5368709120
```

Important:

- `POSTGRES_PASSWORD` and the password inside `DATABASE_URL` must be exactly the same.
- Use three different secret values.
- Each secret must be 32+ random characters.
- Do not use the sample values in production.

Coolify note:

When PostgreSQL and Redis are created through `docker-compose.yml`, they may not appear under Coolify's separate `Databases` page. They are Compose services inside the same deployment. Check them under the deployment's services/containers/logs instead.

## Step 3: Persistent storage

In Coolify, add persistent storage for the API and worker.

Recommended mount:

```text
Mount path: /data
```

At minimum, the following path must persist:

```text
/data/media
```

Why this matters:

- uploaded source videos are stored there
- MP4 previews are stored there
- thumbnails are stored there
- edited clips are stored there
- backup `.tar` files are stored there

If this is not persistent, videos can disappear after redeploy.

## Step 4: Configure public service URLs

In Coolify service/domain settings:

API:

```text
Service: api
Port: 4000
Domain: https://api.yourdomain.com
```

Admin:

```text
Service: admin
Port: 3000
Domain: https://admin.yourdomain.com
```

PostgreSQL, Redis, and worker should not have public domains.

## Step 5: Deploy

Click Deploy in Coolify.

Watch logs for:

```text
api
admin
worker
postgres
redis
```

The API container automatically runs Prisma migrations before starting.

## Step 6: Create the first admin account

Open the Coolify terminal for the `api` container.

Run:

```bash
pnpm --filter @securestream/api admin:create
```

If the command asks for:

```text
Email
Display name
Password
```

enter your admin details.

Password must be at least 12 characters.

Alternative bootstrap method:

Temporarily set:

```env
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_NAME=Admin
BOOTSTRAP_ADMIN_PASSWORD=your-strong-password
```

Redeploy, run:

```bash
pnpm --filter @securestream/api admin:create
```

Then remove those bootstrap variables and redeploy again.

## Step 7: First login test

Open:

```text
https://admin.yourdomain.com
```

Login using your admin account.

Then check:

1. Overview loads.
2. System status updates.
3. Settings opens.
4. Uploads opens.
5. Users opens.
6. Backup & Restore opens.

## Step 8: Test video flow

In admin:

1. Go to Uploads.
2. Upload one small MP4.
3. Wait for preview conversion.
4. Publish the video.
5. Create or confirm a viewer user.
6. Assign the video/folder to that viewer if strict access is enabled.

In Android:

1. Set API base URL:

```text
https://api.yourdomain.com/api/v1
```

2. Login as viewer.
3. Confirm catalog appears.
4. Tap Play Now.
5. Test offline download if enabled.

## Optional alerts

Discord:

```env
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...
```

Generic webhook:

```env
ALERT_WEBHOOK_URL=https://your-webhook-url
```

Telegram:

```env
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

After setting alert env vars, redeploy API and test:

```text
Admin Panel -> Settings -> Test alert
```

## Optional Google Drive backup upload

Set:

```env
GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON=
GOOGLE_DRIVE_FOLDER_ID=
```

`GOOGLE_DRIVE_SERVICE_ACCOUNT_JSON` can be raw JSON or base64 JSON.

Then test:

```text
Admin Panel -> Backup & Restore -> Google Drive
```

## Maintenance mode

To pause viewers while keeping admin access:

```text
Admin Panel -> Settings -> Maintenance mode -> Save settings
```

Viewer apps will be paused.

Admins can still login and turn it off.

## Moving to a new Coolify server/domain

On the new server:

1. Deploy the same GitHub repo.
2. Restore or copy persistent `/data/media`.
3. Restore PostgreSQL or use SecureStream backup restore.
4. Update:

```env
ADMIN_ORIGIN=https://new-admin-domain.com
API_PUBLIC_URL=https://new-api-domain.com
NEXT_PUBLIC_API_URL=https://new-api-domain.com/api/v1
```

5. Redeploy with build.

Important:

`NEXT_PUBLIC_API_URL` is baked into the admin build. If it changes, the admin service must rebuild.

## Common Coolify problems

### Admin cannot login or stays loading

Check:

```env
ADMIN_ORIGIN=https://admin.yourdomain.com
NEXT_PUBLIC_API_URL=https://api.yourdomain.com/api/v1
```

Then rebuild/redeploy admin.

### Upload succeeds but video disappears after redeploy

Persistent storage is missing or mounted to the wrong path.

Fix:

```text
API mount path: /data
Worker mount path: /data
STORAGE_LOCAL_ROOT=/data/media
```

### Android catalog loads but playback fails

Check:

- video is published
- video asset is ready
- viewer has access
- Android API base URL is `https://api.yourdomain.com/api/v1`
- Android uses returned `manifestUrl` exactly as returned

### Backup restore fails

Use only backup files created by SecureStream:

```text
securestream-backup-*.tar
```

The API validates the backup before restoring.

### Discord alert does not work

Check:

```env
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...
```

Redeploy API, then press:

```text
Settings -> Test alert
```

## Coolify final checklist

- Docker Compose resource is used.
- `api` exposes port `4000`.
- `admin` exposes port `3000`.
- PostgreSQL and Redis are private.
- API and worker have persistent `/data`.
- `.env.production.example` values were copied and edited.
- Three JWT/playback secrets are unique and 32+ characters.
- `ADMIN_ORIGIN` matches the admin domain exactly.
- `NEXT_PUBLIC_API_URL` ends with `/api/v1`.
- API deploy logs show migrations completed.
- Admin account is created.
- One test video can upload, publish, and play.
- One backup can be created and downloaded.

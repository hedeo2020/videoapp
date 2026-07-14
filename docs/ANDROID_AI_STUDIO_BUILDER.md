# SecureStream Android guide for Google AI Studio

Use this guide when building or updating the Android viewer app in Google AI Studio.

The Android app is viewer-only. Do not add admin upload, admin user management, backup, or server settings features to Android.

## Import the GitHub repo first

In Google AI Studio:

1. Import/connect this GitHub repository:

```text
https://github.com/hedeo2020/videoapp.git
```

2. Use branch:

```text
main
```

3. Android project folder:

```text
apps/android
```

4. After AI Studio loads the repo, paste the API URL setup script below.

## API base URL

AI Studio will not automatically know your API URL.

You must give it the API base URL:

```text
https://api.yourdomain.com/api/v1
```

Replace `api.yourdomain.com` with your real Coolify API domain.

Store this URL in one Android config/constant value, for example:

```kotlin
const val API_BASE_URL = "https://api.yourdomain.com/api/v1"
```

Use this base URL only for normal API endpoints.

Important playback rule:

When `POST /playback/sessions` returns `manifestUrl`, use `manifestUrl` exactly as returned.

Do not prepend `API_BASE_URL` to `manifestUrl`.

## Copy-ready script to set the API URL after importing the repo

Paste this into Google AI Studio after it imports the GitHub repo:

```text
Update the Android app API base URL.

Project folder:
apps/android

Production API base URL:
https://api.3dbpoint.com/api/v1

Tasks:

1. Find the Android networking/config file that stores the API base URL.
2. If no single config exists, create one Kotlin config object, for example:

   package com.securestream.viewer

   object AppConfig {
       const val API_BASE_URL = "https://api.3dbpoint.com/api/v1"
   }

3. Replace every hardcoded API base URL with AppConfig.API_BASE_URL.
4. Make sure Retrofit/OkHttp uses AppConfig.API_BASE_URL for normal API endpoints.
5. Keep the trailing slash handling correct. If Retrofit requires a trailing slash, use:

   https://api.3dbpoint.com/api/v1/

   but keep the logical API base as:

   https://api.3dbpoint.com/api/v1

6. Do not use the admin URL in the Android app.
7. Do not use:

   https://cpanel.3dbpoint.com

8. Critical playback/download rule:
   - Use API_BASE_URL only for normal endpoints like /auth/login, /catalog, /search, /playback/sessions, /offline/downloads.
   - When the API returns manifestUrl, use manifestUrl exactly as returned.
   - When the API returns downloadUrl, use downloadUrl exactly as returned.
   - Never combine API_BASE_URL + manifestUrl.
   - Never combine API_BASE_URL + downloadUrl.

9. Keep the whole app immersive fullscreen.
10. Keep user logged in permanently unless logout or API rejects the account/session.
11. Do not log tokens, playback URLs, download URLs, or DRM data.

After changing the URL, build the APK and test:

- Login
- Catalog load
- Search
- Play Now
- Download offline
- Reopen app and confirm login remains
```

## Copy-ready AI Studio prompt

Paste this into Google AI Studio when asking it to build/update the Android app:

```text
Build/update the Android viewer app for SecureStream.

Use Kotlin, Jetpack Compose, Retrofit/OkHttp, DataStore or Room, and AndroidX Media3 ExoPlayer.
Minimum SDK: 26.
Branding: SecureStream, dark theme, mint accent color.

API base URL:
https://api.yourdomain.com/api/v1

Store the API base URL in one config constant so it can be changed later.

The app is viewer-only.
Do not add admin upload, admin dashboard, backup, server settings, or user-management admin features.

Required features:

1. Login screen
   - POST /auth/login
   - Body: email, password, deviceId
   - Store accessToken and refreshToken securely.
   - Keep user logged in permanently unless the user taps logout or the API rejects the account/session.

2. Account/session behavior
   - Use stored token on app restart.
   - If API returns ACCOUNT_INACTIVE, ACCOUNT_BLOCKED, TOKEN_INVALID, or 401/403 for account status, clear tokens and show the admin message if provided.
   - Add a logout button in user/account dashboard.

3. App config
   - GET /app/config on startup when internet is available.
   - If maintenance.enabled is true, show maintenance.message and block online viewing.
   - If android.required is true and local versionCode is below android.latestVersionCode, show update required screen with android.message and android.downloadUrl.

4. Home/catalog
   - GET /catalog
   - Display rails/folders returned by the API.
   - Show video thumbnails/posters when available.
   - Do not show admin-only content.

5. Search
   - GET /search?q=<query>
   - Show matching videos/folders.

6. Movie/video detail
   - Show title, synopsis, thumbnail/poster, duration, and Play Now button.
   - Use the word Video, not Movie, in UI labels.

7. Online playback
   - Before online playback call POST /playback/sessions.
   - Body:
     {
       "videoId": "<catalog video id>",
       "deviceId": "<stable device id>",
       "riskSignals": []
     }
   - Use the returned manifestUrl exactly as returned.
   - Do not prepend the API base URL to manifestUrl.
   - Pass returned headers into Media3/ExoPlayer HTTP data source.
   - If licenseUrl exists, configure Widevine DRM.
   - Keep FLAG_SECURE enabled during playback.
   - Support vertical and horizontal orientation/fullscreen playback.

8. Watch progress
   - Periodically POST /playback/progress while online playback is active.
   - Also send progress on pause/stop.

9. Offline downloads
   - Use POST /offline/downloads.
   - Body:
     {
       "videoId": "<catalog video id>",
       "deviceId": "<stable device id>"
     }
   - Download from returned downloadUrl.
   - downloadUrl is already a full URL. Do not prepend API base URL.
   - Store downloaded files only in Android app-private storage.
   - Do not use public Downloads, MediaStore, Gallery, or DownloadManager.
   - Show realtime percent progress by reading the response stream in chunks.
   - Add Downloads screen with Play Offline and Delete actions.

10. Offline mode
   - App must open without internet.
   - If offline, do not block on login refresh, catalog, search, or playback-session API calls.
   - Show Offline mode banner.
   - Show only downloaded videos from local Room/DataStore metadata.
   - Offline playback must play local private file directly and must not call /playback/sessions.
   - Store offline watch progress locally and sync when internet returns.

11. Notifications/messages/dashboard
   - Add user account/dashboard screen.
   - Show user profile/account controls.
   - Show chat/messages with admin and notifications if endpoints are implemented in the current project.
   - Chat layout: admin messages on left, user messages on right.
   - Use Asia/Manila timezone formatting for chat time if possible.

Security rules:

- Do not log access tokens, refresh tokens, playback URLs, download URLs, or DRM license data.
- Keep FLAG_SECURE on playback screens.
- Downloaded videos stay inside app-private storage only.
- Do not expose downloaded files in File Explorer/Gallery.

Playback critical rule:

Use API_BASE_URL for normal endpoints only.
Use returned manifestUrl/downloadUrl exactly as returned.
Never combine API_BASE_URL + manifestUrl.
```

## API endpoint contract

All normal endpoints use:

```text
API_BASE_URL=https://api.yourdomain.com/api/v1
```

### Login

```http
POST /auth/login
```

Body:

```json
{
  "email": "viewer@example.com",
  "password": "password",
  "deviceId": "stable-device-id"
}
```

Expected response includes tokens and user info.

Store tokens securely.

### App config

```http
GET /app/config
```

Use this for:

- maintenance mode
- required Android update/version check

### Catalog

```http
GET /catalog
```

Use bearer access token.

### Search

```http
GET /search?q=query
```

Use bearer access token.

### Playback session

```http
POST /playback/sessions
```

Body:

```json
{
  "videoId": "video-or-movie-id",
  "deviceId": "stable-device-id",
  "riskSignals": []
}
```

Response includes:

```json
{
  "sessionId": "...",
  "manifestUrl": "https://api.yourdomain.com/api/v1/media/.../manifest?pt=...",
  "headers": {
    "X-Playback-Session": "..."
  },
  "licenseUrl": null,
  "videoAssetId": "...",
  "watermark": {
    "text": "abcd..1234"
  },
  "expiresAt": "..."
}
```

Critical:

- `manifestUrl` is already complete.
- Use it exactly.
- Do not add base URL again.

### Progress

```http
POST /playback/progress
```

Body:

```json
{
  "videoAssetId": "asset-id",
  "positionSeconds": 120,
  "completed": false
}
```

### Offline download grant

```http
POST /offline/downloads
```

Body:

```json
{
  "videoId": "catalog-video-id",
  "deviceId": "stable-device-id"
}
```

Response includes:

```json
{
  "videoAssetId": "...",
  "downloadUrl": "https://api.yourdomain.com/api/v1/media/.../download?dt=...",
  "bytesExpected": 123456789,
  "headers": {},
  "expiresAt": "...",
  "storage": "APP_PRIVATE_ONLY"
}
```

Critical:

- `downloadUrl` is already complete.
- Use it exactly.
- Do not add base URL again.

## Realtime download progress rule

Do not use a one-shot file save that only reports when finished.

Read the response stream in chunks:

```kotlin
val expected = grant.bytesExpected ?: response.body.contentLength()
var downloaded = 0L
val buffer = ByteArray(8 * 1024)

while (true) {
    val read = input.read(buffer)
    if (read == -1) break
    output.write(buffer, 0, read)
    downloaded += read
    val percent = if (expected > 0) ((downloaded * 100) / expected).toInt() else null
    updateDownloadProgress(videoId, downloaded, expected, percent)
}
```

UI examples:

```text
Downloading... 37%
Saving offline video...
Download complete
```

## Offline storage rule

Save videos only in:

```kotlin
context.filesDir
```

or:

```kotlin
context.noBackupFilesDir
```

Recommended subfolder:

```text
securestream_offline/
```

Do not use:

- public Downloads folder
- Gallery
- MediaStore
- external shared folders
- Android DownloadManager

## Error handling rules

If the API returns these, logout the viewer locally and show a friendly message:

```text
ACCOUNT_INACTIVE
ACCOUNT_BLOCKED
TOKEN_INVALID
401
403
```

If the response has:

```json
{
  "accountStatus": "DELETED",
  "adminMessage": "..."
}
```

show `adminMessage`.

## Smoke test checklist

Before calling the APK final, test:

1. Login as viewer.
2. Close app and reopen; user stays logged in.
3. Catalog shows published videos.
4. Search works.
5. Video details use label `Video`, not `Movie`.
6. Play Now works.
7. Fullscreen/landscape playback works.
8. Watch progress syncs.
9. Download shows realtime percent.
10. Offline mode opens without internet.
11. Offline videos play without calling API.
12. Delete downloaded video removes the private file.
13. Deleted/disabled user is logged out automatically.
14. Maintenance mode shows the maintenance message.
15. Required Android update blocks old app version.
16. No tokens or playback/download URLs appear in logs.

## What to change when API domain changes

Change only the Android config constant:

```kotlin
API_BASE_URL = "https://new-api-domain.com/api/v1"
```

Then rebuild the APK.

Do not change the playback code that uses returned `manifestUrl`.

## Current production notes

Use the API URL from your Coolify API service.

Example:

```text
https://api.yourdomain.com/api/v1
```

If your admin panel is:

```text
https://admin.yourdomain.com
```

that is not the Android API URL.

Android must use the API domain, not the admin domain.

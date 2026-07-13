# SecureStream Android Offline Downloads

Use this flow for in-app-only offline videos:

1. Show a `Download` button only when a catalog card has `offlineAvailable: true`.
2. When the viewer taps download, call:

   `POST /api/v1/offline/downloads`

   Body:

   ```json
   {
     "videoId": "<catalog movie id>",
     "deviceId": "<stable app device id>"
   }
   ```

3. The API returns `downloadUrl`, `videoAssetId`, `bytesExpected`, and `expiresAt`.
4. Download the file with OkHttp into Android app-private storage only:

   - Use `context.noBackupFilesDir/securestream_offline/` or `context.filesDir/securestream_offline/`.
   - Do not use `Downloads`, `MediaStore`, public external storage, or `DownloadManager`.
   - Name files by `videoAssetId`, not by title, for example `<videoAssetId>.mp4`.
   - Save metadata in DataStore/Room: title, artwork, duration, downloadedAt, expiresAt, localPath.

## Realtime download progress

Downloads must show realtime progress instead of staying at 0% until complete.

Required behavior:

1. After `POST /offline/downloads`, read `bytesExpected` from the response.
2. Start the download with OkHttp.
3. Do not use a one-shot body save that only reports completion.
4. Read the response stream in chunks, for example 8 KB, and write each chunk to the private file.
5. After every chunk:

   - Add the chunk size to `bytesDownloaded`.
   - Calculate `progress = (bytesDownloaded * 100 / bytesExpected).toInt()`.
   - Update the UI state immediately.
   - Show text like `Downloading... 37%`.

6. If `bytesExpected` is missing or zero, fall back to the response `Content-Length` header.
7. If both are unavailable, show downloaded MB instead of percent.
8. When the file completes, verify `bytesDownloaded >= bytesExpected` when `bytesExpected > 0`.
9. Only then mark the download as complete in Room/DataStore.
10. If the download fails or is cancelled, delete the partial file or mark it as resumable.

Pseudo-code:

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

5. Play offline with Media3 ExoPlayer using `MediaItem.fromUri(localFile.toUri())`.
6. Keep `FLAG_SECURE` enabled on playback screens.
7. Add a Downloads screen inside the app with Play and Delete actions.
8. Delete the private file when the user taps Delete, logs out, or when your offline policy expires.

## Offline mode when there is no internet

The Android app must continue to open even when the device has no internet connection.

Required behavior:

1. Detect network state with `ConnectivityManager` / `NetworkCallback`.
2. If the app starts offline:

   - Do not block on login refresh, catalog, search, or playback-session API calls.
   - Show an `Offline mode` banner.
   - Load only downloaded video metadata from Room/DataStore.
   - Show one home rail named `Downloaded videos`.
   - Hide online-only catalog folders, search results, featured rails, and stream-only videos.
   - Disable or hide Download buttons.
   - Allow Play and Delete for downloaded videos.

3. If the app goes offline while already open:

   - Keep the current screen open.
   - Show `Offline mode`.
   - Any online API action should fail gracefully with `Internet connection required`.
   - Existing offline videos must remain playable.

4. Offline playback:

   - Play from the private local file path saved during download.
   - Do not call `POST /playback/sessions` for offline playback.
   - Do not call `POST /playback/progress` while offline; store local progress and sync it later when online.
   - Keep `FLAG_SECURE` enabled.

5. When internet returns:

   - Refresh auth token if needed.
   - Refresh online catalog.
   - Sync locally stored watch progress for downloaded videos.
   - Keep downloaded videos available in the Downloads screen.

Suggested UI copy:

- Banner: `Offline mode`
- Empty offline library: `No downloaded videos yet. Connect to the internet and download videos to watch offline.`
- Blocked action message: `Internet connection required for this action.`

Important security note: app-private storage hides videos from normal File Explorer and Gallery apps. It is not the same as full DRM. A rooted phone or device backup tooling may still access app-private files. For stronger protection later, add encrypted local storage or Widevine offline licenses.

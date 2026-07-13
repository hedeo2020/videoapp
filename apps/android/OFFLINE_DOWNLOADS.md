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

3. The API returns `downloadUrl`, `videoAssetId`, and `expiresAt`.
4. Download the file with OkHttp into Android app-private storage only:

   - Use `context.noBackupFilesDir/securestream_offline/` or `context.filesDir/securestream_offline/`.
   - Do not use `Downloads`, `MediaStore`, public external storage, or `DownloadManager`.
   - Name files by `videoAssetId`, not by title, for example `<videoAssetId>.mp4`.
   - Save metadata in DataStore/Room: title, artwork, duration, downloadedAt, expiresAt, localPath.

5. Play offline with Media3 ExoPlayer using `MediaItem.fromUri(localFile.toUri())`.
6. Keep `FLAG_SECURE` enabled on playback screens.
7. Add a Downloads screen inside the app with Play and Delete actions.
8. Delete the private file when the user taps Delete, logs out, or when your offline policy expires.

Important security note: app-private storage hides videos from normal File Explorer and Gallery apps. It is not the same as full DRM. A rooted phone or device backup tooling may still access app-private files. For stronger protection later, add encrypted local storage or Widevine offline licenses.

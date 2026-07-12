# Android viewer

The viewer is Kotlin/Compose with a minimum SDK of 26. `MainActivity.enterProtectedPlayback()` applies `FLAG_SECURE` before protected frames are shown; player dialogs and overlays must remain in that protected window. Media3 dependencies cover HLS, DASH, and Widevine configuration. Playback URLs are requested from the API and must never be persisted or logged. No download service, offline cache, cast integration, sharing action, or default picture-in-picture capability is included.

Production builds enable shrinking and obfuscation. Integrate Play Integrity through a repository boundary and submit its verdict-derived risk signals to the server; failures must not crash the app. Risk signals are advisory and server policy may deny, warn, or audit. Forensic watermark text must use only the server-approved masked value.

Build with `./gradlew :app:assembleDebug` or `./gradlew :app:bundleRelease` after supplying an Android SDK and release signing configuration outside source control.

# SecureStream Android local build and USB install

Use this when Google AI Studio is unavailable or you want to build the same Android app locally.

## What is locked in

- Android project: `apps/android`
- Gradle wrapper: `apps/android/gradlew.bat`
- App name: `SecureStream`
- App package: `com.aistudio.securestream.vqzklp`
- Minimum SDK: `26`
- API URL currently compiled in: `https://compreface.3dbpoint.com/api/v1/`

## Requirements

Install Android Studio first, then open it once so it installs:

- Android SDK
- Platform tools / ADB
- Android build tools
- JDK 17 or newer

On this Windows machine the paths used were:

```powershell
$env:ANDROID_HOME = "C:\Users\PC\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
```

If your SDK or Java is installed somewhere else, change those paths before building.

## Build debug APK

From the repo root:

```powershell
cd apps\android
$env:ANDROID_HOME = "C:\Users\PC\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
.\gradlew.bat assembleDebug --no-daemon
```

The APK will be created here:

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

For convenience, copy it to:

```powershell
cd ..\..
mkdir dist -Force
copy apps\android\app\build\outputs\apk\debug\app-debug.apk dist\SecureStream-debug.apk
```

## Install to USB phone

On the phone:

1. Enable Developer options.
2. Enable USB debugging.
3. Connect USB cable.
4. Accept the debugging prompt on the phone.

Then run:

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices -l
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r dist\SecureStream-debug.apk
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell monkey -p com.aistudio.securestream.vqzklp -c android.intent.category.LAUNCHER 1
```

If `adb devices -l` shows no device, reconnect the cable, switch USB mode, or toggle USB debugging off/on.

## If the old placeholder app is still installed

The older local build used a different package:

```text
com.securestream.viewer
```

You can remove it with:

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" uninstall com.securestream.viewer
```

This does not remove the new AI Studio package:

```text
com.aistudio.securestream.vqzklp
```

## Verify the APK

```powershell
& "$env:ANDROID_HOME\build-tools\36.1.0\aapt.exe" dump badging dist\SecureStream-debug.apk
```

Expected key values:

```text
package: name='com.aistudio.securestream.vqzklp'
application-label:'SecureStream'
sdkVersion:'26'
targetSdkVersion:'36'
```

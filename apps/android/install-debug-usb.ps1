$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$apkPath = Join-Path $repoRoot "dist\SecureStream-debug.apk"

if (-not $env:ANDROID_HOME) {
  $defaultSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
  if (Test-Path $defaultSdk) {
    $env:ANDROID_HOME = $defaultSdk
  } else {
    throw "ANDROID_HOME is not set. Install Android Studio or set ANDROID_HOME to your Android SDK path."
  }
}

$adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
  throw "ADB not found at $adb"
}

if (-not (Test-Path $apkPath)) {
  throw "APK not found at $apkPath. Build first with: cd apps\android; .\gradlew.bat assembleDebug"
}

& $adb devices -l
& $adb install -r $apkPath
& $adb shell monkey -p "com.aistudio.securestream.vqzklp" -c "android.intent.category.LAUNCHER" 1

# Build, export to platform-tools\APKs, and optionally install to connected ADB device
$ErrorActionPreference = "Stop"

$toolsRoot = if (Test-Path "D:\platform-tools") { "D:\platform-tools" } else { "C:\platform-tools" }
$APKsFolder = Join-Path $toolsRoot "APKs"
if (!(Test-Path $APKsFolder)) {
    New-Item -ItemType Directory -Path $APKsFolder | Out-Null
}

$env:JAVA_HOME = Join-Path $toolsRoot "jdk"
$androidSdk = if (Test-Path "D:\Android\sdk") { "D:\Android\sdk" } else { "C:\Users\HP\AppData\Local\Android\Sdk" }
$env:ANDROID_HOME = $androidSdk
$env:PATH = "$env:JAVA_HOME\bin;$toolsRoot\MinGit\cmd;$toolsRoot;$androidSdk\platform-tools;$env:PATH"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Building Aniyomi Plus (arm64-v8a)...  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$animetailRoot = Join-Path $toolsRoot "Animetail"
Set-Location $animetailRoot

# 1. Clear old build outputs so stale APKs cannot be accidentally copied
if (Test-Path "app\build\outputs\apk") {
    Remove-Item -Path "app\build\outputs\apk" -Recurse -Force
}

# 2. Compile updated code
.\gradlew.bat :app:assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[ERROR] Gradle build failed! Check the errors above." -ForegroundColor Red
    exit 1
}

$generatedApk = Get-ChildItem -Path "app\build\outputs\apk" -Recurse -Filter "*.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if ($generatedApk) {
    $destFile = Join-Path $APKsFolder "AniyomiPlus-arm64-v8a-debug.apk"

    Copy-Item -Path $generatedApk.FullName -Destination $destFile -Force

    Write-Host "`n[SUCCESS] Successfully exported APK to:" -ForegroundColor Green
    Write-Host "   $destFile" -ForegroundColor Yellow

    # Check if an ADB device is connected
    $adbCmd = if (Test-Path "$androidSdk\platform-tools\adb.exe") { "$androidSdk\platform-tools\adb.exe" } else { "adb" }
    $devices = & $adbCmd devices | Select-String "device$"
    if ($devices) {
        Write-Host "`n[ADB] Found connected device, installing APK..." -ForegroundColor Cyan
        & $adbCmd install -r $destFile
        Write-Host "[LAUNCH] Launching Aniyomi Plus..." -ForegroundColor Green
        & $adbCmd shell am start -n com.dark.animetailv2.custom.dev/eu.kanade.tachiyomi.ui.main.MainActivity
    }
} else {
    Write-Host "[ERROR] No APK found in build outputs!" -ForegroundColor Red
}

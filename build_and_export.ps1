# Build, export to C:\platform-tools\APKs, and optionally install to connected ADB device
$ErrorActionPreference = "Stop"

$APKsFolder = "C:\platform-tools\APKs"
if (!(Test-Path $APKsFolder)) {
    New-Item -ItemType Directory -Path $APKsFolder | Out-Null
}

$env:JAVA_HOME = "C:\platform-tools\jdk"
$env:PATH = "$env:JAVA_HOME\bin;C:\platform-tools\MinGit\cmd;C:\platform-tools;$env:PATH"
$env:ANDROID_HOME = "C:\Users\HP\AppData\Local\Android\Sdk"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Building Aniyomi Plus (arm64-v8a)...  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Set-Location "C:\platform-tools\Animetail"
.\gradlew.bat :app:assembleDebug

$generatedApk = Get-ChildItem -Path "app\build\outputs\apk" -Recurse -Filter "*.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if ($generatedApk) {
    $destFile = Join-Path $APKsFolder "AniyomiPlus-arm64-v8a-debug.apk"
    
    Copy-Item -Path $generatedApk.FullName -Destination $destFile -Force
    
    Write-Host "`n[SUCCESS] Successfully exported APK to:" -ForegroundColor Green
    Write-Host "   $destFile" -ForegroundColor Yellow
    
    # Check if an ADB device is connected
    $devices = adb devices | Select-String "device$"
    if ($devices) {
        Write-Host "`n[ADB] Found connected device, installing APK..." -ForegroundColor Cyan
        adb install -r $destFile
        Write-Host "[LAUNCH] Launching Aniyomi Plus..." -ForegroundColor Green
        adb shell am start -n com.dark.animetailv2.custom.dev/eu.kanade.tachiyomi.ui.main.MainActivity
    }
} else {
    Write-Host "[ERROR] No APK found in build outputs!" -ForegroundColor Red
}

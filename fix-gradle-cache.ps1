# Run this script AFTER restarting your PC (with Cursor and Android Studio closed)
# Opens a new PowerShell, stops Gradle, removes corrupted cache, then rebuilds

$transformsPath = "$env:USERPROFILE\.gradle\caches\8.13\transforms"

Write-Host "Stopping Gradle daemons..."
Set-Location $PSScriptRoot
& .\gradlew --stop 2>$null

Start-Sleep -Seconds 3

Write-Host "Removing corrupted transforms cache..."
if (Test-Path $transformsPath) {
    Remove-Item -Recurse -Force $transformsPath -ErrorAction Stop
    Write-Host "Cache removed successfully."
} else {
    Write-Host "Transforms folder not found (already removed?)."
}

Write-Host "Building project..."
& .\gradlew clean assembleDebug

Read-Host "Press Enter to close"

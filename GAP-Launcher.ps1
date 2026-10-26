$ErrorActionPreference = "Stop"
$AppUrl = "https://gap.davidmayer.workers.dev/"

# Prefer the installed Chromium browser, including Opera GX, in app-window mode.
$Candidates = @(
  "$env:LOCALAPPDATA\Programs\Opera GX\opera.exe",
  "$env:ProgramFiles\Opera GX\opera.exe",
  "$env:ProgramFiles(x86)\Opera GX\opera.exe",
  "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
  "$env:ProgramFiles(x86)\Google\Chrome\Application\chrome.exe",
  "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe",
  "$env:ProgramFiles(x86)\Microsoft\Edge\Application\msedge.exe",
  "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
  "$env:LOCALAPPDATA\Microsoft\Edge\Application\msedge.exe"
)
$Browser = $Candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if ($Browser) {
  Start-Process -FilePath $Browser -ArgumentList @("--app=$AppUrl")
} else {
  # Fallback only if no supported Chromium browser is installed.
  Start-Process $AppUrl
}

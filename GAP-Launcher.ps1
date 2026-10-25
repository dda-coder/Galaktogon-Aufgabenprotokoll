$ErrorActionPreference = "Stop"
$AppUrl = "https://galaktogon-projekt.davidmayer.workers.dev/"
$Candidates = @(
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
  Start-Process $AppUrl
}

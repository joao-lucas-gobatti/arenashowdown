param([Parameter(Mandatory=$true)][string[]]$Commands)
$ErrorActionPreference='Stop'
$channel=Join-Path $PSScriptRoot '..\server\.console-input'
for($attempt=0;$attempt -lt 30;$attempt++) {
    try { Add-Content -LiteralPath $channel -Value $Commands -ErrorAction Stop; break }
    catch { if($attempt -eq 29){throw}; Start-Sleep -Milliseconds 150 }
}

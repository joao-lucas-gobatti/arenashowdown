$ErrorActionPreference = 'Stop'
$InputFile = Join-Path (Split-Path -Parent $PSScriptRoot) 'server\.console-input'
if (-not (Test-Path -LiteralPath $InputFile)) { throw 'Canal de console não encontrado; inicie com scripts/start-server.ps1.' }
Add-Content -LiteralPath $InputFile -Value 'stop'

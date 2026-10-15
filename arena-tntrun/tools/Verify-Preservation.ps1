param([string]$BaselinePath = (Join-Path $PSScriptRoot '../.work/baseline.json'))
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
Push-Location -LiteralPath $repo
try {
    if ((git branch --show-current) -ne 'dev/rafael') { throw 'Branch must remain dev/rafael' }
    $baseline = Get-Content -LiteralPath $BaselinePath -Raw | ConvertFrom-Json
    foreach ($entry in $baseline) {
        if (!(Test-Path -LiteralPath $entry.Path -PathType Leaf)) { throw ('Missing original file: ' + $entry.Path) }
        if ((Get-FileHash -LiteralPath $entry.Path -Algorithm SHA256).Hash -ne $entry.SHA256) { throw ('Changed original file: ' + $entry.Path) }
    }
    $initial = (Get-Content -LiteralPath (Join-Path $PSScriptRoot '../.work/initial-head.txt') -Raw).Trim()
    $committed = @(git diff --name-only $initial HEAD)
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect commit diff' }
    if ($committed | Where-Object { !$_.StartsWith('arena-tntrun/') }) { throw 'Commit range contains paths outside arena-tntrun' }
    $staged = @(git diff --cached --name-only)
    if ($staged.Count -ne 0) { throw 'Unexpected staged files remain' }
    Write-Output ('SHA256 unchanged: ' + $baseline.Count + ' original files outside arena-tntrun')
    Write-Output ('Commit range paths: ' + $committed.Count + ', all exclusively arena-tntrun/')
    git log --oneline ($initial + '..HEAD')
    git diff --stat $initial HEAD
} finally { Pop-Location }

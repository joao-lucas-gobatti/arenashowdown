$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$JavaExe = Join-Path $ProjectRoot '.tools\jdk-21\bin\java.exe'
$ServerDir = Join-Path $ProjectRoot 'server'
$PaperJar = Join-Path $ServerDir 'paper-1.21.11-132.jar'
if (-not (Test-Path -LiteralPath $JavaExe)) { throw 'JDK local ausente. Consulte o README.' }
if (-not (Test-Path -LiteralPath $PaperJar)) { throw 'Paper ausente. Consulte o README.' }
$CommandFile = Join-Path $ServerDir '.console-input'
Set-Content -LiteralPath $CommandFile -Value ''
$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $JavaExe
$startInfo.Arguments = '-Xms1G -Xmx2G -jar "paper-1.21.11-132.jar" --nogui'
$startInfo.WorkingDirectory = $ServerDir
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardInput = $true
$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
$process.Start() | Out-Null
Set-Content -LiteralPath (Join-Path $ServerDir '.server-pid') -Value $process.Id
$processedLines = 0
try {
    while (-not $process.HasExited) {
        $lines = @(Get-Content -LiteralPath $CommandFile -ErrorAction SilentlyContinue)
        if ($lines.Count -gt $processedLines) {
            $lines[$processedLines..($lines.Count - 1)] | ForEach-Object {
                if ($_.Trim().Length -gt 0) { $process.StandardInput.WriteLine($_) }
            }
            $processedLines = $lines.Count
        }
        Start-Sleep -Milliseconds 100
    }
    $process.WaitForExit()
    exit $process.ExitCode
} finally {
    if (-not $process.HasExited) { $process.StandardInput.WriteLine('stop'); $process.WaitForExit(30000) | Out-Null }
}

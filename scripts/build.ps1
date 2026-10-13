$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = Join-Path $ProjectRoot '.tools\jdk-21'
& (Join-Path $ProjectRoot 'gradlew.bat') clean build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

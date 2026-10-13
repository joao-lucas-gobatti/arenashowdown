$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $MyInvocation.MyCommand.Path
$bundledJdk = Join-Path $project '..\.tools\jdk-21'
if (Test-Path (Join-Path $bundledJdk 'bin\java.exe')) {
    $env:JAVA_HOME = (Resolve-Path $bundledJdk).Path
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}
& "$project\..\gradlew.bat" -p $project clean build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Plugin criado em cassino-lobby\build\libs\CassinoLobby-1.0.0.jar"


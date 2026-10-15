param([string]$SeedServer = 'server-test-delivery3')
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$module = Join-Path $repo 'arena-tntrun'
$target = [IO.Path]::GetFullPath((Join-Path $module '.test-server'))
if (!$target.StartsWith($module + [IO.Path]::DirectorySeparatorChar)) { throw 'Target outside module' }
if (Test-Path -LiteralPath $target) { throw 'Test server already exists; this script never overwrites an existing server.' }
$seed = (Resolve-Path -LiteralPath (Join-Path $repo $SeedServer)).Path
if (!(Test-Path -LiteralPath (Join-Path $seed 'paper.jar'))) { throw 'Missing Paper bootstrap JAR' }
if ((Get-Content -LiteralPath (Join-Path $seed 'eula.txt') -Raw) -notmatch 'eula=true') { throw 'Seed server must already have an accepted EULA.' }
New-Item -ItemType Directory -Path $target | Out-Null
# Copy only executable dependencies. No worlds, maps, plugin data, player data or databases.
foreach ($name in @('paper.jar','cache','libraries','versions','eula.txt')) {
    Copy-Item -LiteralPath (Join-Path $seed $name) -Destination $target -Recurse
}
$plugins = Join-Path $target 'plugins'
New-Item -ItemType Directory -Path $plugins | Out-Null
Copy-Item -LiteralPath (Join-Path $module 'build/libs/ArenaTNTRun-0.1.0-SNAPSHOT.jar') -Destination $plugins
Copy-Item -LiteralPath (Join-Path $module 'build/libs/ArenaTNTRunVerification-0.1.0-SNAPSHOT.jar') -Destination $plugins
Copy-Item -LiteralPath (Join-Path $module '.work/core-build/libs/ArenaCore-0.2.0-SNAPSHOT.jar') -Destination $plugins
$arenaDirectory = Join-Path $plugins 'ArenaTNTRun/arenas'
New-Item -ItemType Directory -Path $arenaDirectory -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $module 'src/main/resources/arena.example.yml') -Destination (Join-Path $arenaDirectory 'teste.yml')
$properties = @'
server-ip=127.0.0.1
server-port=25679
online-mode=false
enable-rcon=false
enable-query=false
level-name=tntrun_test
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains","structures":{}}
generate-structures=false
view-distance=2
simulation-distance=2
spawn-protection=0
max-players=4
motd=ArenaTNTRun isolated verification
'@
[IO.File]::WriteAllText((Join-Path $target 'server.properties'), $properties)
Write-Output $target
Write-Output 'Start with an interactive console: java -Darenatntrun.isolated=true -Dterminal.jline=false -Dterminal.ansi=false -Xms512M -Xmx1G -jar paper.jar --nogui'
Write-Output 'Console: tntrunverify, tntrun list, tntrun validate teste, stop'

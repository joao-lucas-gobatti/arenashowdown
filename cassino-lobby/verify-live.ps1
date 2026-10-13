$ErrorActionPreference = 'Stop'
$console = Join-Path $PSScriptRoot '..\server\.console-input'
$checks = @(
    'execute in minecraft:cassino_lobby_v2 if block 0 66 22 minecraft:red_terracotta run say V2_CHECK spawn OK',
    'execute in minecraft:cassino_lobby_v2 if block 27 80 19 minecraft:smooth_quartz_stairs run say V2_CHECK escada OK',
    'execute in minecraft:cassino_lobby_v2 if block 34 81 16 minecraft:air run say V2_CHECK acesso-galeria OK',
    'execute in minecraft:cassino_lobby_v2 if block 68 67 0 minecraft:air run say V2_CHECK lounge OK',
    'execute in minecraft:cassino_lobby_v2 if block -68 67 0 minecraft:air run say V2_CHECK fama OK',
    'execute in minecraft:cassino_lobby_v2 if block 0 105 0 minecraft:deepslate_tiles run say V2_CHECK teto OK',
    'execute in minecraft:cassino_lobby_v2 if block 0 105 51 minecraft:black_stained_glass run say V2_CHECK dado OK',
    'execute in minecraft:cassino_lobby_v2 if entity @e[type=minecraft:text_display,tag=cassino_lobby_managed] run say V2_CHECK displays OK'
)
foreach ($x in @(-43,43)) {
    foreach ($z in @(-24,-8,8,24)) {
        $checks += "execute in minecraft:cassino_lobby_v2 if block $x 67 $z minecraft:air run say V2_CHECK portal $x $z OK"
    }
}
for ($attempt=0; $attempt -lt 30; $attempt++) {
    try {
        Add-Content -LiteralPath $console -Value $checks -ErrorAction Stop
        break
    } catch {
        if ($attempt -eq 29) { throw }
        Start-Sleep -Milliseconds 150
    }
}
Write-Host 'Verificações enviadas. Confira 16 mensagens V2_CHECK em server/logs/latest.log.'

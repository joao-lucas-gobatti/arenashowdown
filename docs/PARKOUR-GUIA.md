# Guia do ArenaParkour

## Instalação

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools\jdk-21').Path
.\gradlew.bat clean build --no-parallel
Copy-Item arena-core\build\libs\ArenaCore-0.2.0-SNAPSHOT.jar server\plugins\ArenaCore.jar -Force
Copy-Item arena-parkour\build\libs\ArenaParkour-0.3.0-SNAPSHOT.jar server\plugins\ArenaParkour.jar -Force
.\scripts\start-server.ps1
```

Confirme em `/plugins` que ArenaCore e ArenaParkour estão verdes.

## Mundo e pista de teste

```text
/parkour buildtest parkour-test
/parkour validate parkour-test
```

Isso cria somente o mundo `parkour_test`, com dez obstáculos, cinco checkpoints, chegada, área de queda e plataforma de espectadores. O YAML fica em `plugins/ArenaParkour/arenas/parkour-test.yml`.

## Arena manual

```text
/parkour create minha-pista
/parkour setstart minha-pista
/parkour addspawn minha-pista
/parkour addspawn minha-pista
/parkour pos1
/parkour pos2
/parkour addcheckpoint minha-pista
/parkour pos1
/parkour pos2
/parkour setfinish minha-pista
/parkour setfallheight minha-pista 50
/parkour setlobby minha-pista
/parkour validate minha-pista
/parkour save minha-pista
/parkour test minha-pista
```

Repita `pos1`, `pos2` e `addcheckpoint` na ordem do percurso. Checkpoints e chegada são regiões 3D. Para outro nome, altere `plugins/ArenaCore/minigames.yml` com o servidor parado:

```yaml
minigames:
  parkour:
    enabled: true
    arena: minha-pista
```

## Campeonato individual

Com dois jogadores conectados:

```text
/arena champ create copa01 individual parkour Copa Arena Showdown
/arena register copa01 JogadorUm
/arena register copa01 JogadorDois
/arena status copa01
/arena next copa01
/arena ranking copa01
```

## Campeonato por equipes

```text
/arena champ create copa-times teams parkour Copa por Equipes
/arena team create copa-times vermelho Vermelho
/arena team create copa-times azul Azul
/arena register copa-times JogadorUm
/arena register copa-times JogadorDois
/arena team assign copa-times JogadorUm vermelho
/arena team assign copa-times JogadorDois azul
/arena next copa-times
/arena team ranking copa-times
```

## Cancelar e reiniciar

```text
/arena match list copa01
/arena match cancel copa01-r1 motivo administrativo
/arena match restart copa01-r1 copa01-r1-retry
```

IDs de resultado e eventos `match:<id>:...` evitam pontos duplicados.

## Solução de problemas

- **Arena incompleta:** use `/parkour validate <arena>` e confira `minigames.yml`.
- **Jogador não encontrado:** conecte-o ou use a sintaxe antiga com UUID.
- **Participante offline:** a preparação é cancelada sem registrar resultado.
- **Queda do servidor:** snapshots ficam em `plugins/ArenaParkour/snapshots`; ao reconectar, inventário, modo e localização são restaurados, e o arquivo só é removido depois do sucesso.
- **Pontuação:** o ArenaCore aplica `plugins/ArenaCore/scoring.yml`; ArenaParkour nunca escreve no SQLite.

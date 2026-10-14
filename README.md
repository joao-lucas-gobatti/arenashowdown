# Arena Showdown

> A Entrega 4 adiciona `arena-lobby`: scoreboard, hologramas, GUI, interações e proteções no mapa existente, sem regenerá-lo. Consulte [docs/GUIA-LOBBY.md](docs/GUIA-LOBBY.md).

Plataforma modular de campeonatos para Paper 1.21.11 e Java 21. O repositório contém o `ArenaCore`, a API pública e o primeiro minigame jogável, `ArenaParkour`.

## Build e instalação

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools\jdk-21').Path
.\gradlew.bat clean build --no-parallel
Copy-Item arena-core\build\libs\ArenaCore-0.2.0-SNAPSHOT.jar server\plugins\ArenaCore.jar -Force
Copy-Item arena-parkour\build\libs\ArenaParkour-0.3.0-SNAPSHOT.jar server\plugins\ArenaParkour.jar -Force
.\scripts\start-server.ps1
```

O ArenaCore cria `plugins/ArenaCore/arena.db` automaticamente e mantém a pontuação centralizada no SQLite. O ArenaParkour não acessa o banco.

## Módulos

- `arena-api`: contratos públicos, incluindo `MinigameAdapter` e `OrchestrationApi`.
- `arena-core`: campeonatos, partidas, ranking, pontuação idempotente, SQLite e comandos `/arena`.
- `arena-parkour`: arenas YAML, corrida, checkpoints, timeout, scoreboard e resultados automáticos.

## Fluxo rápido do Parkour

Entre como operador e gere a pista isolada:

```text
/parkour buildtest parkour-test
/parkour validate parkour-test
```

Crie o campeonato e cadastre jogadores conectados:

```text
/arena champ create copa01 individual parkour Copa Arena Showdown
/arena register copa01 JogadorUm
/arena register copa01 JogadorDois
/arena next copa01
/arena ranking copa01
```

O `/arena next` valida o adaptador, a arena, participantes e duplicidade antes de criar a rodada. A pista controla preparação, contagem de cinco segundos, corrida e envio do resultado ao ArenaCore.

## Comandos novos

- `/arena register <campeonato> <nickname>`; a forma antiga com UUID continua válida.
- `/arena team assign <campeonato> <nickname-ou-uuid> <equipe>`.
- `/arena next <campeonato>` prepara a próxima rodada sem duplicá-la.
- `/arena status <campeonato>` mostra estado, rodada, próximo jogo, participantes, partida ativa e ranking.
- `/parkour help` lista os comandos de configuração.

Consulte [docs/PARKOUR-GUIA.md](docs/PARKOUR-GUIA.md) e [docs/TEST-REPORT.md](docs/TEST-REPORT.md).

## Limitações

- A corrida completa ainda precisa ser validada manualmente com dois clientes Minecraft reais.
- O gerador da pista roda apenas por `/parkour buildtest` e nunca altera o `cassino-lobby`.
- Desconexão durante a corrida cancela a partida; retomada no mesmo ponto ainda não é oferecida.
- O retorno usa o snapshot persistido da localização original, não um lobby visual definitivo.

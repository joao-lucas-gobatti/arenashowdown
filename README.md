# Arena Showdown

Núcleo modular de competições para Paper 1.21.11. A Entrega 2 permite executar campeonatos individuais ou por equipes com minigames simulados; mapas e minigames reais continuam fora do escopo.

## Requisitos e build

- Windows 10/11 x64
- JDK 21; o ambiente local usa Temurin em `.tools/jdk-21`
- Paper 1.21.11 build 132 em `server/paper-1.21.11-132.jar`

```powershell
.\scripts\build.ps1
```

O artefato final é `arena-core/build/libs/ArenaCore-0.2.0-SNAPSHOT.jar`. Gradle é fornecido pelo Wrapper.

## Execução local

```powershell
Copy-Item arena-core\build\libs\ArenaCore-0.2.0-SNAPSHOT.jar server\plugins\ArenaCore.jar -Force
.\scripts\start-server.ps1
```

Em outro PowerShell, encerre com `./scripts/stop-server.ps1`. O servidor principal escuta somente em `127.0.0.1:25565`.

## Arquitetura

- `arena-api`: contratos públicos, modelos de campeonato/partida e `MinigameAdapter`.
- `ArenaService`: API anterior de inscrição, pontos por event-id e ranking; permanece compatível.
- `ChampionshipManager`: modo individual/equipes, sequência de jogos, pausa, retomada, campeão e histórico.
- `MatchManager`: calendário, máquina de estados, conexões e submissão idempotente de resultados.
- `ArenaRepository` e `OrchestrationRepository`: isolam toda persistência da lógica e dos minigames.
- `JdbcArenaRepository` e `JdbcOrchestrationRepository`: implementação SQLite atual.
- `PlayerSessionManager`: snapshots temporários de inventário, localização, modo, vida e alimentação, sempre na thread principal.

Plugins de minigame obtêm `ArenaApi` e `OrchestrationApi` pelo `ServicesManager` do Bukkit. Eles implementam `MinigameAdapter` com `prepare`, `start`, `finish`, `cancel`, `reset`, `getResult` e `getParticipants`; não recebem acesso ao banco.

## Estados

Campeonato:

`WAITING → PREPARING → RUNNING ⇄ PAUSED → FINISHED`

Partida:

`WAITING → PREPARING → COUNTDOWN → RUNNING → FINISHED`

Campeonatos e partidas ativos também podem ser cancelados conforme as transições validadas no código.

## Comandos principais

Compatibilidade anterior:

- `/arena create <id> <nome>`
- `/arena prepare|start|pause|resume|finish|cancel <id>`
- `/arena join <campeonato> [equipe]`
- `/arena score <campeonato> <jogador-ou-uuid> <pontos> <origem> <event-id>`
- `/arena ranking <campeonato>`

Campeonatos e equipes:

- `/arena champ create <id> <individual|teams> <gamesCSV> <nome>`
- `/arena champ details <id>`
- `/arena register <campeonato> <uuid> <nome> [equipe]`
- `/arena team create <campeonato> <id> <nome>`
- `/arena team assign <campeonato> <uuid> <equipe>`
- `/arena team ranking <campeonato>`

Partidas:

- `/arena match create <id> <campeonato> <rodada> <minigame> <arena> <uuidCSV>`
- `/arena match prepare|countdown|start <id>`
- `/arena match finish <id> <result-id> <uuid:pontos[:equipe],...>`
- `/arena match cancel <id> [motivo]`
- `/arena match status <id>`
- `/arena match list <campeonato>`
- `/arena match restart <partida-antiga> <nova-partida>`
- `/arena match abandon <id> <uuid>`

Administração exige `arena.admin`; permissões-filhas documentam campeonato, equipes, partidas e pontuação. Jogadores usam `arena.play`.

## Pontuação e desempate

`plugins/ArenaCore/scoring.yml` define pontos globais e específicos por minigame. Por padrão, o ArenaCore ignora pontos enviados pelo adaptador e recalcula pelo posicionamento. `use-result-points: true` permite resultados personalizados.

Desempates disponíveis: `MOST_WINS`, `BEST_LAST_ROUND`, `EARLIEST_REGISTRATION` e `NAME`. O padrão é `MOST_WINS`.

A finalização oficial é uma única transação: grava resultado, entradas, pontuação individual/equipe, duração, rodada atual e auditoria. IDs de resultado e eventos `match:<id>:...` impedem reaplicação após repetição ou reinício.

## SQLite, YAML e backups

O banco `plugins/ArenaCore/arena.db` é criado/migrado automaticamente. SQLite usa WAL, foreign keys e timeout de concorrência. Operações de banco e backup são assíncronas.

- `config.yml`: banco, ranking e retenção de backups.
- `minigames.yml`: ativação e opções futuras dos jogos.
- `scoring.yml`: pontuação e desempate.

Cada inicialização cria um snapshot consistente com `VACUUM INTO`, valida `PRAGMA integrity_check` e conserva as cópias configuradas.

## Limitações atuais

- Snapshots de inventário/localização são mantidos em memória; uma queda abrupta durante partida exige recuperação operacional manual.
- Não existe temporizador automático para converter desconexão em abandono; partidas sem reconexão marcam abandono imediato e há comando administrativo.
- O retorno provisório usa o spawn do mundo atual, não um lobby visual definitivo.
- Não há mapas, painel web, Velocity ou minigames completos.
- A instância de teste não teve clientes Minecraft reais conectados; eventos de conexão foram cobertos na camada de orquestração.

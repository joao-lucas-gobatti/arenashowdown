# Guia do ArenaLobby

Compile com `./gradlew.bat build --no-parallel`, copie `arena-lobby/build/libs/ArenaLobby-0.4.0-SNAPSHOT.jar` para `server/plugins/ArenaLobby.jar` e desative `CassinoLobby.jar` para evitar dois conjuntos de displays/proteções. Inicie com `scripts/start-server.ps1`. O mundo deve existir; ArenaLobby nunca gera mapas.

## Comandos

- `/lobby` ou `/lobby menu`: GUI dos oito minigames.
- `/lobby setspawn` e `/lobby spawn`.
- `/lobby setchampionship <id>`.
- `/lobby hologram create <id> <ranking|championship|last>`; `move <id>`; `remove <id>`.
- `/lobby interaction create <id> <menu|ranking|status|admin>`; `remove <id>`.
- `/lobby reload` e `/lobby status`.

Configuração exige `arena.lobby.admin`; consultas usam `arena.lobby.use`. Os comandos possuem autocompletar.

## Parkour e ranking

Escolha um campeonato existente e abra a GUI. Operadores veem **Iniciar próxima rodada**, que chama a validação oficial de `/arena next <id>`. ArenaCore aciona ArenaParkour quando for a próxima modalidade. O snapshot do Parkour devolve o jogador ao local original; iniciando no lobby, ele retorna ao lobby. Os visuais atualizam em até cinco segundos.

## Diagnóstico

- `world=false`: confira `world` no config e o `level.dat`.
- Sem dados: valide `/arena status <id>` e use `/lobby setchampionship <id>`.
- Parkour indisponível: confirme o plugin ativo e a arena validada.
- Não execute CassinoLobby e ArenaLobby juntos. `/lobby reload` reconstrói somente displays marcados pelo ArenaLobby.
- O scoreboard do Parkour tem prioridade durante a corrida; o do lobby reaparece no retorno.

A estética e a circulação devem ser aprovadas manualmente no cliente Minecraft.

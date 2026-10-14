# Inspeção do lobby existente — Entrega 4

Data: 14/10/2026. Inspeção feita com o servidor desligado, pelos arquivos, fontes e log do Paper. Não houve inspeção visual por cliente Minecraft.

- `server/cassino_lobby` é a V1 preservada; `server/cassino_lobby_v2` é diferente e é o mundo apontado pela configuração e carregado no log mais recente.
- Spawn projetado da V2: `(0.5, 68, 22.5)`. O projeto documenta cerca de 190×190 blocos, fachada, salão central, piso geométrico, colunas, mezaninos, palco, cortinas, lustre, Hall da Fama, lounge e oito portais.
- Pontos previstos: palco/placar perto de `(0, 78, -35)`, Hall da Fama em `(-50, 73, 0)` e lounge em `(50, 73, 0)`.
- `CassinoLobby` carregava o mundo, displays, proteções e portais. ArenaLobby substitui apenas o runtime e não possui gerador de blocos.

Antes da implementação, a V1 foi copiada para `backups/delivery4-20261014-105411/cassino_lobby`. O manifesto cobre 16 arquivos e tem SHA-256 `e5694046cb4053166af87727db57d6b4f44d5ee66c4bac3c8ceb5359c0214390`.

Sugestões não executadas: avaliar a legibilidade do painel pelo spawn e mezaninos, limitar a três hologramas, associar interações invisíveis à decoração existente e conferir luz/câmeras no cliente antes de mudanças arquitetônicas.

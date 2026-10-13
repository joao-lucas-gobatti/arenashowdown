# Cassino Lobby — Arena Showdown

**V2:** consulte [V2.md](V2.md) para arquitetura nova, backup, recuperação e pontos de inspeção. A primeira inicialização migra o lobby para um mundo separado com sufixo `_v2`, preservando o mundo anterior e os destinos configurados.

Plugin Paper 1.21.11 autônomo que cria um mundo vazio e gera, em lotes, um lobby art déco de cassino com cerca de 190×190 blocos. Tudo desta entrega está contido nesta pasta.

## Conteúdo

- praça e escadaria cinematográfica, fachada e letreiro;
- salão central com piso geométrico, colunas, mezaninos, palco, cortinas e lustre;
- oito portais temáticos configuráveis;
- placar em `TextDisplay`, integrado opcionalmente ao ranking do ArenaCore;
- Hall da Fama, lounge VIP, dados, fichas e iluminação perimetral;
- mundo vazio dedicado, proteção básica e geração incremental;
- marcador persistente contra geração duplicada.

## Compilar e instalar

Na raiz do repositório:

```powershell
.\cassino-lobby\build.ps1
Copy-Item .\cassino-lobby\build\libs\CassinoLobby-1.0.0.jar .\server\plugins\ -Force
.\scripts\start-server.ps1
```

Entre como OP e execute:

```text
/cassino generate
/cassino tp
```

A construção é aplicada por lotes (padrão: 6.000 operações/tick). Em uma máquina com 16 GB, o valor é conservador; ajuste `generation.blocks-per-tick` no `config.yml` do plugin se necessário. O arquivo `generated-v1.marker` impede execução duplicada. Para reaplicar intencionalmente: `/cassino regenerate CONFIRMAR`.

## Configuração

Após a primeira execução, edite `server/plugins/CassinoLobby/config.yml`:

- `world.name` e `world.base-y`: mundo e altura da construção;
- `scoreboard.championship-id`: campeonato consultado no ArenaCore;
- `scoreboard.title`, `fallback-lines` e `max-entries`: conteúdo do painel;
- `portals.<id>.title`: nome exibido;
- `portals.<id>.command`: comando executado pelo jogador ao entrar (sem `/`; aceita `%player%`).

Para salvar um destino físico, fique no ponto desejado e use `/cassino setportal <id>`. Isso grava mundo, XYZ, yaw e pitch e limpa o comando daquele portal. IDs: `corrida`, `skywars`, `battle_royale`, `gamble`, `parkour`, `roube_a_bandeira`, `csgo`, `tnt_run`.

O mapa nasce em `(0.5, base-y + 4, 22.5)`, olhando para o palco e placar. Jogadores novos são enviados automaticamente; jogadores existentes usam `/cassino tp`.

## Comandos e segurança

- `/cassino generate` — primeira geração;
- `/cassino regenerate CONFIRMAR` — sobrescreve apenas os blocos do projeto na mesma região;
- `/cassino tp` — teleporta ao spawn;
- `/cassino setportal <id>` — captura a posição atual;
- `/cassino reload` e `/cassino status`.

Todos exigem `cassino.admin`, liberado por padrão para facilitar o servidor local vinculado a `127.0.0.1`. Em uma publicação externa, altere `default` para `op` no `plugin.yml`. Dano e alterações de blocos são bloqueados no lobby; administradores em Criativo continuam podendo editar.

## Código para personalização visual

- `LobbyPlan.java`: coordenadas, materiais, setores e portais;
- `config.yml`: textos, ranking e destinos;
- `LobbyDisplays.java`: letreiros e painel dinâmico;
- `LobbyListener.java`: proteção e detecção dos portais.


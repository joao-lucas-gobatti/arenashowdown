# ArenaTNTRun

Reconstrução local em `dev/rafael`, em 14/10/2026. Paper **1.21.11**, Java **21**.
O identificador público é **`tntrun`**, registrado na `OrchestrationApi` fornecida pelo ArenaCore.
Não há dependência de ArenaParkour nem alteração nas APIs compartilhadas.

## Build

Na raiz do repositório, em PowerShell:

```powershell
$env:GRADLE_USER_HOME = 'C:\Users\Rafael\.gradle'
.\gradlew.bat :arena-tntrun:clean :arena-tntrun:build `
  --init-script arena-tntrun/isolated-build.gradle `
  --project-cache-dir arena-tntrun/.gradle-isolated --offline
```

O init script direciona as saídas de ArenaAPI, ArenaCore e raiz para `arena-tntrun/.work/`.
Isso preserva os artefatos e caches locais previamente modificados. `--offline` exige dependências
já disponíveis; em uma máquina nova, remova essa opção para permitir o download normal do Gradle.
O build gera também um plugin separado de verificação. Ele **não** integra o JAR de produção.

Artefato de produção: `build/libs/ArenaTNTRun-0.1.0-SNAPSHOT.jar` dentro deste módulo.
O JAR usa a API carregada pelo ArenaCore; não inclui cópias da ArenaAPI.

## Instalação em servidor isolado

1. Pare o servidor de testes. Use Paper 1.21.11 e Java 21, com ArenaCore instalado.
2. Coloque somente `ArenaTNTRun-0.1.0-SNAPSHOT.jar` em `plugins/`.
3. Inicie uma vez para gerar `plugins/ArenaTNTRun/config.yml` e a pasta `arenas/`.
4. Configure uma arena em um **mundo de testes próprio**, já construído. O plugin não cria
   mundos nem constrói mapas. O exemplo `src/main/resources/arena.example.yml` não é ativado automaticamente.
5. Copie/adapte o exemplo para `plugins/ArenaTNTRun/arenas/teste.yml` e execute `/tntrun reload`.
   O `id` deve corresponder ao nome do arquivo. Use `/tntrun validate teste` e confira o mundo.
6. Na configuração **da cópia de testes** do ArenaCore, acrescente a chave abaixo em `minigames.yml`.
   O arquivo distribuído pelo Core contém `tnt-run`, que é um identificador diferente.

```yaml
minigames:
  tntrun:
    enabled: true
    arena: teste
```

Não se altera automaticamente a configuração do Core. Pontos são determinados pela política do Core:
por padrão, 10/7/5 para as três primeiras posições. Para personalizar, use `scoring.minigames.tntrun.positions`
na cópia de testes. O plugin envia pontos zero para que o Core aplique sua política.

Exemplo de campeonato pelo console, com dois jogadores **online** no servidor de testes:

```text
arena champ create copa-tnt individual tntrun Copa TNT
arena register copa-tnt <UUID1> Jogador1
arena register copa-tnt <UUID2> Jogador2
arena next copa-tnt
arena status copa-tnt
arena ranking copa-tnt
```

As inscrições precedem o início. `/arena next` prepara a partida; o adapter controla a contagem
de cinco segundos e a transição para RUNNING. Não use `/arena match start` para antecipar a contagem:
essa tentativa cancela e restaura a partida.

## Configuração e comandos

Cada arena define mundo, cubo de proteção, camadas, spawns, ponto de espectador, limite de queda,
timeout, atraso de remoção e materiais. As camadas são ordenadas pela altura, de cima para baixo.
`depth: 2` remove a superfície e o suporte correspondente (por exemplo, areia e TNT).
Não há explosão, drops ou atualização de física na remoção/restauração.
O piso desaparece também sob jogadores parados. A área amostrada inclui as bordas dos pés.

Materiais aceitos: SAND, RED_SAND, GRAVEL, TNT, STONE, SMOOTH_STONE, SANDSTONE,
RED_SANDSTONE, TERRACOTTA, lãs, concretos e terracotas coloridas/esmalteadas.
Contêineres/tile entities são recusados na preparação. Spawns precisam estar sobre o piso superior,
com espaço para corpo e cabeça. Mínimo de dois jogadores; um spawn por jogador.

| Comando | Função |
| --- | --- |
| `/tntrun list`, `/tntrun status` | Arenas e ciclo atual |
| `/tntrun create <id>` | Criar rascunho YAML no mundo atual |
| `/tntrun pos1`, `/tntrun pos2` | Selecionar coordenadas usando a posição dos pés |
| `/tntrun bounds <id>` | Definir cubo usando as duas posições |
| `/tntrun layer <id> [depth]` | Acrescentar camada; ambas as posições no mesmo Y |
| `/tntrun spawn <id>` | Acrescentar spawn na posição dos pés |
| `/tntrun spectator <id>` | Definir posição de espectador dentro do cubo |
| `/tntrun rules <id> <timeout-seconds> <delay-ticks> <elimination-y>` | Definir regras |
| `/tntrun materials <id> <SAND,TNT,...>` | Definir materiais de piso |
| `/tntrun save <id>` | Validar e ativar rascunho; não altera blocos |
| `/tntrun reload`, `/tntrun validate <id>` | Recarregar/validar YAML |
| `/tntrun cancel [motivo]`, `/tntrun recover` | Cancelar ou tentar recuperação/reenvio |
| `/tntrun spectate`, `/tntrun leave` | Assistir/sair; participante abandona e vira espectador |

Rascunhos `.draft` são persistentes e não são carregados como arenas. Edições exigem que não exista
partida nem recuperação de blocos/resultados pendente. Reload inválido preserva a configuração validada
anterior. Arenas sobrepostas são recusadas. A validação YAML não substitui a inspeção física na preparação.

Permissões: `arenatntrun.admin` (padrão op), `arenatntrun.spectate` e `arenatntrun.leave` (padrão true).
Scoreboard e mensagens da partida são configuráveis em `config.yml`, com placeholders no exemplo.
Textos são tratados literalmente, sem interpretar comandos ou MiniMessage.

## Regras de partida e proteção

- Preparação exige todos online, vivos, sem veículo/cama e sem snapshot individual pendente.
  Visitantes dentro da arena devem sair antes da preparação.
- Contagem fixa de 100 ticks (cinco segundos a 20 TPS), com participantes imóveis e piso intacto.
- Queda abaixo de `elimination-y`, saída do cubo/mundo, morte, voo/modo incompatível ou desconexão
  eliminam. Desconexão na preparação/contagem cancela. Reconexão não devolve a condição de vivo,
  mesmo quando o Core permite reconexões.
- Até um sobrevivente: vitória. Timeout: sobreviventes vêm antes dos eliminados, primeiro na camada
  mais alta e depois por UUID textual crescente. Eliminados: tick de eliminação decrescente, depois UUID.
  Posições são únicas e determinísticas. Queda simultânea de todos produz classificação sem vencedor.
- Participantes eliminados permanecem espectadores até o término. Espectadores externos podem sair
  imediatamente e têm estado próprio salvo/restaurado.
- Proteção de blocos cobre o cubo configurado mesmo fora de partidas: quebra, colocação, interação,
  pistões, líquidos, física, fogo, crescimento, explosões e priming de TNT. Durante a partida também
  bloqueia dano, fome, inventário, drops/coleta, comandos de participantes sem admin e teleporte externo.
  Espectadores ficam contidos no cubo e não mantêm alvo de espectador.

## Recuperação

`plugins/ArenaTNTRun/recovery/` guarda exclusivamente dados próprios do TNT Run:

- `session.txt`: partida em preparação/execução até confirmação terminal no Core;
- `blocks.yml`: todos os BlockData originais dos pisos, com UUID do mundo;
- `players/<uuid>.yml`: localização/mundo, modo, inventário completo (armadura/offhand), cursor,
  slot, saúde, absorção, alimentação, experiência, efeitos, voo, velocidades, colisão, invulnerabilidade,
  gravidade, corrida, agachamento, natação, voo de elytra, fogo, congelamento, queda, velocidade vetorial e scoreboard;
- `result.yml`: resultado com id estável `<match>-tntrun-v1`, antes de limpar a partida e enviar ao Core.

Arquivos são escritos antes da primeira alteração. Escritas usam arquivo temporário e flush do conteúdo;
originais pendentes nunca são sobrescritos. Jogador é salvo antes de consumir o snapshot; mundo é salvo
antes de consumir o journal de blocos. Restauração pode ser repetida após interrupção.
No início do plugin, restaura os pisos e jogadores online e cancela a partida interrompida no Core.
Jogadores offline recuperam no próximo login. Resultados pendentes são reenviados ou reconhecidos
quando o Core já contém o mesmo id. Conflito de resultado, arquivo inválido ou mundo ausente mantém
os arquivos para intervenção; não há descarte automático.

`/tntrun recover` tenta novamente; `WorldLoadEvent` também tenta recuperar blocos pendentes.
Falha individual impede nova captura daquele jogador; o jogador pode ser desconectado para impedir
uso do estado incompleto. Não apague snapshots nem resultados para liberar uma partida.

## Testes e limites

Veja [docs/validation.md](docs/validation.md) para resultados executados e [docs/initial-status.txt](docs/initial-status.txt)
para o estado inicial. Os testes são novos desta reconstrução.

- Um adapter atende **uma partida por vez**, conforme o contrato singleton do Core. Não prepare/cancele
  outra partida `tntrun` simultaneamente por comandos baixos do Core: `cancel/reset` não recebem match id
  na API compartilhada. O plugin recusa uma segunda preparação e não sobrescreve a partida atual.
- Operações Bukkit são na thread principal. Captura/flush e restauração de até 65.536 blocos são síncronos;
  arenas grandes podem causar pausa. Recomenda-se começar com pisos pequenos no servidor isolado.
- Tempos usam ticks; lag prolonga a duração de parede. Timeout: 5–3.600s; remoção: 1–100 ticks; profundidade: 1–4.
- Após reinício, scoreboards customizados são reconstruídos com objetivos, scores editáveis, equipes,
  prefixos/sufixos, cores e opções. Scores de critérios automáticos são recalculados pelo servidor.
  Formatações avançadas por score e referências mantidas por outros plugins não são persistidas.
  Sem reinício, restaura a instância original do scoreboard. Inventários de interface abertos são fechados.
- Plugins que editam blocos/estado diretamente podem contornar eventos; não há garantia de convivência
  com editores ou outros minigames que movam os mesmos jogadores simultaneamente.
- Flush reduz a janela de perda em interrupção de processo, mas não é garantia contra falha de disco,
  energia ou corrupção externa. Mantenha backups do servidor de testes.
- A verificação real usou Paper e blocos reais, com um jogador simulado para serialização. Não houve
  partida com clientes Minecraft humanos, teste de carga, nem validação de anticheats/plugins externos.

Nenhum servidor existente recebeu instalação. Nenhum push foi executado.

# Arena Showdown — House of Wonders V3

Relatório de entrega, geração e validação — 2026-10-13.

## 1. Resultado entregue

A V3 é um gerador Paper isolado para um complexo único chamado **The House of Wonders**, com átrio central e quatro alas nas orientações exigidas:

- Norte: Fácil — Candy Casino.
- Leste: Médio — Celestial Card Palace.
- Oeste: Difícil — Crimson Clockwork.
- Sul: Impossível — Royal Dice Dimension.

O plano final contém 12 fases, 42 transições significativas por fase, 504 transições de fase no complexo, 126 transições oficiais por dificuldade e 36 checkpoints seguros. Passagens temáticas caminháveis conectam fisicamente portal → fase 1 → fase 2 → fase 3; elas não inflam artificialmente a contagem de saltos.

Artefatos:

- Código gerador: `src/main/java/com/arenashowdown/parkour/wonder/`.
- JAR: `build/libs/ArenaParkour-0.3.0-SNAPSHOT.jar`.
- SHA-256 do JAR validado: `B9580CAF0A5716A22DA56AF4FE9694871907E66FD9F6E310CBFF039D769B6B65`.
- Tamanho do JAR validado: 167.094 bytes.
- Blueprint: `HOUSE-OF-WONDERS-V3-BLUEPRINT.md`.
- Mundo gerado: `server/house_of_wonders_v3` após `/wonder generate`.
- IDs competitivos: `wonder-easy`, `wonder-medium`, `wonder-hard`, `wonder-impossible`, servidos pelo adaptador existente `parkour`.

## 2. Preservação da V2

A V3 não escreve em `grand_dice_palace`, `palace-v1.*`, arenas `palace-*` ou `palace-records.yml`. O Palace continua acessível por `/palace`.

Backup pré-V3:

- `cassino-lobby/backups/parkour-v2-pre-house-of-wonders-v3-20261013.zip`
- SHA-256: `EF8C373F2292F2BA3CD21A7FAC946B691089093B213703BC812849A859B0F41C`

Arquivos V3 isolados no data folder:

- `wonder-v3.pending`: propriedade e retomada de uma geração incompleta.
- `wonder-v3.complete`: manifesto atômico com schema, blueprint e UUID do mundo.
- `wonder-v3.arenas-ready`: gate publicado somente após as quatro arenas serem salvas e conferidas.
- `wonder-v3-snapshots/`: recuperação de jogadores em treino.
- `wonder-v3-records.yml`: melhores tempos e quedas.
- `wonder-v3-coordinates.tsv`: exportação produzida por `/wonder report`.

## 3. Métricas reais do plano

| Métrica | Resultado validado |
|---|---:|
| Fases | 12 |
| Transições por fase | 42 |
| Transições de fase totais | 504 |
| Transições oficiais por ala | 126 |
| Checkpoints seguros | 9 por ala / 36 total |
| Pousos arquitetônicos validados | 516, incluindo as entradas de capítulo |
| Materiais distintos no plano | 87 |
| Posições voxel planejadas | 137.391, incluindo carving intencional de AIR |
| Bounds reais | X -221..209, Y 58..184, Z -212..220 |
| Envelope real | 431 × 127 × 433 |
| Layouts espaciais distintos | 12 |

O átrio usa roseta radial, não piso xadrez. A implementação não reutiliza o shell de corredor do Palace. As cenas usam componentes específicos: dado chanfrado orbital, lustres, jardins, fonte, cartas monumentais, castelo de cartas, nuvens, engrenagens, relógios, teatro, balcões, tubos, roda mecânica, anfiteatro, prisma-espelho, cristais, dado fraturado, anéis, escadaria impossível e coroa.

## 4. Lista das fases e mecânicas

| Ala | Fase | Transições | Gramática de percurso | Landmark e mecânicas |
|---|---|---:|---|---|
| Fácil | Confetti Garden | 42 | serpentina de jardim | fichas largas, pontes, flores, fonte, degraus curvos e bordas de dado |
| Fácil | Giant Toy Room | 42 | órbita ascendente | torre de dados-brinquedo, relógio, blocos, engrenagens; subir, contornar e descer |
| Fácil | Rainbow Ballroom | 42 | galerias perimetrais | lustre, cortinas, balcões, mezaninos e plataformas 2×2 nos cantos críticos |
| Médio | Floating Card Kingdom | 42 | zigue-zague entre torres | castelo de cartas, nuvens, ilhas, diagonais e contorno aéreo |
| Médio | Sky Clockwork | 42 | anéis/espiral de relógio | mostrador, sete engrenagens, ponteiros, dentes e passarelas circulares |
| Médio | Celestial Mirror Hall | 42 | reversões espelhadas | prisma, cristais, arcos, mudanças de direção e altura moderadas |
| Difícil | Broken Theatre | 42 | vigas e balcões | palco quebrado, cortinas, rigging, trapdoors e precisão prolongada |
| Difícil | Mechanical Wonder | 42 | mecanismo vertical | roleta/roda, tubos, engrenagens, dentes irregulares e mudanças rápidas |
| Difícil | Grand Showdown | 42 | bowl de anfiteatro | arena multinível, estátuas, vigas, anéis e ascensão final |
| Impossível | Fractured Reality | 42 | órbitas fragmentadas | dado aberto, anéis, cristais, fragmentos e pousos estreitos |
| Impossível | Infinite Palace | 42 | reversões em vários andares | escadaria impossível, arcos, torres, espelhos e precisão avançada |
| Impossível | Crown of Dice | 42 | espiral de ascensão | coroa de 56 blocos, dado suspenso, raios e final em Y=175 |

Slime, honey e ladders não foram incluídos nesta revisão: a prioridade foi manter todas as 504 transições estáticas determinísticas e confiáveis no multiplayer. Nenhum movimento especial é considerado válido implicitamente pelo verificador.

## 5. Coordenadas importantes

Formato: `(X, Y, Z)`. `CP14` e `CP28` são checkpoints intermediários; `Final/CP42` conclui a fase e também é checkpoint. O hub/lobby fica em `(0.5, 65, 8.5)` e o centro cenográfico do átrio em aproximadamente `(0.5, 91, 0.5)`.

| Ala / fase | Entrada | CP14 | CP28 | Final / CP42 |
|---|---|---|---|---|
| Fácil 1 — Confetti Garden | `(-20,69,-48)` | `(-15,69,-57)` | `(-15,70,-67)` | `(-15,71,-77)` |
| Fácil 2 — Giant Toy Room | `(0,105,-110)` | `(4,107,-146)` | `(-9,109,-108)` | `(14,111,-146)` |
| Fácil 3 — Rainbow Ballroom | `(-20,70,-166)` | `(20,71,-173)` | `(1,72,-200)` | `(-20,73,-174)` |
| Médio 1 — Floating Card Kingdom | `(48,74,-15)` | `(62,76,3)` | `(76,77,9)` | `(90,78,-9)` |
| Médio 2 — Sky Clockwork | `(114,79,9)` | `(145,81,2)` | `(113,83,-14)` | `(129,85,22)` |
| Médio 3 — Celestial Mirror Hall | `(162,83,-18)` | `(176,85,12)` | `(190,87,-6)` | `(204,89,0)` |
| Difícil 1 — Broken Theatre | `(-49,77,27)` | `(-70,77,6)` | `(-91,77,27)` | `(-112,78,6)` |
| Difícil 2 — Mechanical Wonder | `(-118,85,0)` | `(-130,92,9)` | `(-134,99,-5)` | `(-118,106,-6)` |
| Difícil 3 — Grand Showdown | `(-177,93,17)` | `(-179,96,-14)` | `(-192,99,8)` | `(-174,102,-1)` |
| Impossível 1 — Fractured Reality | `(24,99,48)` | `(25,99,65)` | `(24,100,83)` | `(25,101,100)` |
| Impossível 2 — Infinite Palace | `(24,109,105)` | `(-18,114,119)` | `(24,119,133)` | `(-18,124,147)` |
| Impossível 3 — Crown of Dice | `(9,133,172)` | `(-11,147,200)` | `(11,161,180)` | `(-11,175,193)` |

Todas as 516 coordenadas de entrada/pouso podem ser exportadas depois da geração com `/wonder report` para `plugins/ArenaParkour/wonder-v3-coordinates.tsv`.

## 6. Compilar, instalar e gerar

### Compilar

No PowerShell, a partir da raiz do workspace:

```powershell
$env:JAVA_HOME = (Resolve-Path ".tools\jdk-21").Path
& ".tools\gradle-9.2.1\bin\gradle.bat" :arena-parkour:build --no-daemon
```

### Instalar com rollback de JAR

Faça isto com o servidor parado. O backup fica fora de `server/plugins` para o Paper não carregar dois plugins com o mesmo nome.

```powershell
New-Item -ItemType Directory -Force "server\plugin-backups"
Copy-Item "server\plugins\ArenaParkour.jar" "server\plugin-backups\ArenaParkour-pre-v3.jar" -Force
Copy-Item "arena-parkour\build\libs\ArenaParkour-0.3.0-SNAPSHOT.jar" "server\plugins\ArenaParkour.jar" -Force
```

Inicie o Paper manualmente, pois é um processo de longa duração:

```powershell
Set-Location "server"
& "..\.tools\jdk-21\bin\java.exe" -Xms2G -Xmx4G -jar "paper-1.21.11-132.jar" --nogui
```

### Gerar e validar no jogo/console

1. Execute `/wonder generate` como OP.
2. Aguarde `WONDER_V3_READY`. Não desligue o servidor durante a aplicação normal; se houver crash, o `pending` permite reaplicação idempotente.
3. Execute `/wonder status`.
4. Execute `/wonder verify`; resultado esperado: `WONDER_VERIFY_OK: 516 pousos, 504 transições, 36 checkpoints, 12 fases.`
5. Execute `/wonder report`.
6. Inspecione o átrio com `/wonder hub`.
7. Inspecione cada capítulo com `/wonder view <easy|medium|hard|impossible> <1|2|3>`.
8. Faça playtest completo com `/wonder start <dificuldade>`.

Comandos de treino:

```text
/wonder hub
/wonder start easy|medium|hard|impossible
/wonder reset
/wonder restart
/wonder leave
/wonder records
/wonder view <ala> <1..3>
```

`reset` retorna ao checkpoint seguro atual e conta queda; `restart` volta ao início, reinicia tempo e quedas; `leave` restaura atomicamente inventário, posição e estado anteriores.

## 7. Teste competitivo ArenaCore

Exemplo para Fácil:

```text
/arena match create v3-easy-test <campeonato> <rodada> parkour wonder-easy <UUIDs-separados-por-virgula>
/arena match prepare v3-easy-test
```

Repita com `wonder-medium`, `wonder-hard` e `wonder-impossible`. O adaptador exige as 126 regiões em ordem, mas salva respawn somente nos nove checkpoints de capítulo. Equipes e snapshots são aguardados juntos; callbacks antigos são invalidados por token; DNF é incluído no resultado sem pontuação na política atual.

## 8. Segurança e desempenho

- A composição voxel é feita fora da thread principal sem acessar objetos vivos do Bukkit.
- Escritas de mundo ocorrem na main thread, sem física.
- Chunks são carregados com `getChunkAtAsync`, recebem ticket temporário e o ticket é removido na troca/finalização.
- Limite por tick: até 2.500 operações e aproximadamente 3 ms.
- Mundo vazio sem noise, caves, estruturas, decoração ou mobs.
- Manifestos usam publicação atômica e vinculam schema, revisão do blueprint e UUID do mundo.
- As arenas competitivas permanecem fechadas até `wonder-v3.arenas-ready` confirmar as quatro definições.
- Snapshots usam arquivo temporário, `fsync`, move atômico, validação de schema/campos/tipos/cardinalidade e deleção somente após aplicação.
- Palace, Wonder e corrida oficial possuem exclusão mútua por jogador.

## 9. Arquivos criados ou alterados

### V3

- `src/main/java/com/arenashowdown/parkour/wonder/WonderBlocks.java`
- `src/main/java/com/arenashowdown/parkour/wonder/WonderRoute.java`
- `src/main/java/com/arenashowdown/parkour/wonder/WonderComponents.java`
- `src/main/java/com/arenashowdown/parkour/wonder/WonderArchitecture.java`
- `src/main/java/com/arenashowdown/parkour/wonder/WonderBuilder.java`
- `src/main/java/com/arenashowdown/parkour/wonder/WonderService.java`
- `src/test/java/com/arenashowdown/parkour/wonder/WonderPrototypeTest.java`
- `src/test/java/com/arenashowdown/parkour/wonder/WonderFullArchitectureTest.java`
- `HOUSE-OF-WONDERS-V3-BLUEPRINT.md`
- `WONDER-V3.md`

### Integração e robustez compartilhada

- `src/main/java/com/arenashowdown/parkour/ArenaParkourPlugin.java`
- `src/main/java/com/arenashowdown/parkour/ParkourAdapter.java`
- `src/main/java/com/arenashowdown/parkour/player/PersistentSnapshotStore.java`
- `src/main/java/com/arenashowdown/parkour/palace/PalaceService.java`
- `src/main/resources/plugin.yml`
- `src/test/java/com/arenashowdown/parkour/player/PersistentSnapshotStoreFileTest.java`

### Backup

- `cassino-lobby/backups/parkour-v2-pre-house-of-wonders-v3-20261013.zip`

Nenhuma alteração intencional foi feita em `server/server.properties`; ele já estava modificado antes deste trabalho.

## 10. Compilação e testes executados

Comando final:

```powershell
$env:JAVA_HOME = (Resolve-Path ".tools\jdk-21").Path
& ".tools\gradle-9.2.1\bin\gradle.bat" :arena-parkour:build --rerun-tasks --no-daemon
```

Resultado: **BUILD SUCCESSFUL**.

- 19 testes.
- 0 falhas.
- 0 erros.
- 7 suítes.
- `git diff --check`: sem erros.
- JAR inspecionado: contém `plugin.yml` e classes `WonderArchitecture`, `WonderBuilder`, `WonderRoute` e `WonderService`.

Validações cobertas:

- 12 fases e indexação global.
- 42 transições por fase e 126 por ala oficial.
- 36 checkpoints.
- 12 assinaturas espaciais distintas.
- Vocabulário de obstáculos.
- Vãos de borda, subidas e quedas por dificuldade.
- Suporte e três blocos de headroom em todo o footprint, nas quatro rotações.
- Bounds, altura, materiais e densidade.
- Landmarks representativos.
- Snapshot preexistente, documento truncado/tipo incorreto e compatibilidade legada completa.
- Testes existentes de arena, RaceTracker e Palace.

A compilação produz avisos de APIs Paper marcadas como deprecated (`GameRule` tipado e `CHORUS_FRUIT`), mas não erros. Parte dos avisos também existe no Palace legado.

## 11. Limitações e validação obrigatória pendente

Não foram executados:

- Geração real do mundo em um servidor Paper ativo.
- Screenshots reais.
- Inspeção visual dentro do Minecraft.
- Playtest humano dos 504 saltos.
- Teste com dois ou mais clientes.
- Corrida ArenaCore completa com campeonato real.
- Medição TPS/heap durante `/wonder generate`.
- Teste de crash forçado em cada ponto do commit.

A validação matemática é conservadora, mas não substitui habilidade humana e percepção visual. Antes de produção, realize as inspeções `/wonder view`, percorra as quatro dificuldades, teste os saltos críticos do Difícil/Impossível e registre screenshots/TPS. Não redirecione portais de produção até esse aceite.

## 12. Rollback

Como o mundo V2 não é substituído, rollback do plugin é direto, com o servidor parado:

```powershell
Copy-Item "server\plugin-backups\ArenaParkour-pre-v3.jar" "server\plugins\ArenaParkour.jar" -Force
```

O diretório `server/house_of_wonders_v3` e dados `wonder-v3*` podem permanecer sem afetar o Palace. Não os apague antes de copiar screenshots, relatórios ou recordes necessários.

# THE GRAND DICE — PARKOUR PALACE

Um mundo separado (`grand_dice_palace`) contém um edifício em cruz, com dimensão máxima de aproximadamente 258×258 blocos. O átrio mede aproximadamente 65×65; as quatro alas avançam até a coordenada local 126. Piso do átrio Y=70, largadas Y=71. Nenhum bloco do lobby é alterado pelo gerador.

## Entrar e jogar

```text
/palace hub
/palace start easy
/palace start medium
/palace start hard
/palace start impossible
/palace reset
/palace records
/palace leave
```

O portal Parkour do lobby usa `palace hub`. Cada ala tem seu letreiro e comando de início. Treinos têm cronômetro, ordem obrigatória de pousos, três checkpoints finais de etapa, retorno após queda, efeito de chegada e recorde pessoal persistido. O painel central exibe o melhor tempo de treino de cada dificuldade. Cada pista contém 21 saltos, totalizando 84 saltos e 12 finais de etapa. As largadas não contam como finais de etapa.

O inventário e o modo anterior são salvos antes do treino e restaurados por `/palace leave`. Se houver desconexão ou reinício, o snapshot é recuperado ao reconectar. Não inicie uma partida oficial antes de sair do treino. Efeitos de poção devem terminar antes da largada. Alterar modo de jogo ou voar encerra a tentativa. O cronômetro inclui as quedas.

O fácil tem plataformas 3×3; o médio, 2×2 com deslocamento lateral; o difícil, 1×1 com mudanças de direção; o impossível usa 1×1 e alternância de altura. O nome não significa que os saltos sejam inalcançáveis. As distâncias foram verificadas geometricamente, **não testadas fisicamente por um jogador**; o balanceamento final requer playtest no cliente.

## Planta e ambientes

| Ala | Direção | Etapas | Cenários dominantes |
|---|---|---|---|
| Easy / Golden Hall | Norte (−Z) | Fichas → Dados → Jardim | Pilhas de fichas, dados chanfrados, jardim com fonte e árvores |
| Medium / Floating Cards | Leste (+X) | Biblioteca → Galeria → Ilusões | Estantes altas, cartas suspensas, painéis de gelo como espelhos |
| Hard / Crimson Theatre | Oeste (−X) | Bastidores → Roleta → Palco | Cortinas e passarelas, roda vertical e engrenagens, palco e tribunas |
| Impossible / Shadow Palace | Sul (+Z) | Labirinto → Abismo → Trono | Dados e arcos, cartas e pontes interrompidas, trono com dado coroado |

O átrio tem abóbada em níveis, dado suspenso, lustre de três camadas, fontes cenográficas de vidro iluminado, quatro entradas com coroamentos distintos, escadas e sacadas. Um recepcionista original com cartola usa um dado como cabeça. Janelas profundas, pilastras externas, cúpulas escalonadas e pavilhões marcam a silhueta. Os percursos ficam sobre suportes temáticos; o piso decorativo inferior é área de queda, não um atalho.

As galerias laterais de observação são acessíveis pelos pontos `/palace view`; não fazem parte dos percursos cronometrados. As sacadas do átrio têm escadas físicas. Os lustres, teto abobadado, arcos e vitrais dão iluminação; todos os pousos têm luz inferior. A faixa central dos saltos é aberta após a composição dos cenários, para preservar espaço de cabeça.

## Coordenadas e checkpoints

Centro: `(0.5, 71, 5.5)`. Largadas: fácil `(0.5,71,-29.5)`, médio `(30.5,71,0.5)`, difícil `(-29.5,71,0.5)`, impossível `(0.5,71,30.5)`.

Na função `PalaceRoute.landings`, a coordenada `v` é a distância do centro na direção da ala; `u` é o deslocamento lateral. Centros das câmaras: `v=46,74,102`. Checkpoints de etapa: `v=58,86,114`, Y dos pés `73,75,77`. O final teleporta o treino para a varanda `v=120`, Y=77.

O arquivo `PalaceRoute.java` é a fonte comum para blocos, regiões e treino. Os YAMLs `plugins/ArenaParkour/arenas/palace-{easy,medium,hard,impossible}.yml` são cadastrados automaticamente uma única vez. Cada YAML tem 21 regiões ordenadas, incluindo os três finais de etapa, nove posições de largada e chegada. Nas partidas oficiais cada região é também um checkpoint intermediário para reduzir repetição. Os arquivos existentes não são sobrescritos ao reiniciar. Alterar a geometria exige atualizar a fonte e gerar uma nova revisão em outro mundo; editar somente YAML altera a corrida oficial, não o treino.

## Partidas oficiais

O mesmo `ParkourAdapter` existente continua registrado em `OrchestrationApi` como `parkour`. Use uma das quatro arenas novas no fluxo já existente:

```text
/arena match create <id> <campeonato> <rodada> parkour palace-easy <UUIDs-separados-por-virgula>
/arena match prepare <id>
```

O campeonato deve estar em estado compatível e seus participantes devem estar inscritos e online. O adaptador faz contagem regressiva, registra posições e envia `MatchResult`; o ArenaCore mantém as regras de pontuação e idempotência. Treino não concede pontos de campeonato. O novo plugin exige a API atual do repositório (incluindo `participants`); o JAR antigo do servidor não tinha esse método e foi salvo antes da atualização.

## Compilar, gerar e validar

```powershell
$env:JAVA_HOME = (Resolve-Path .tools/jdk-21).Path
.\gradlew.bat :arena-core:build :arena-parkour:build
```

Com servidor desligado, instale `ArenaCore-0.2.0-SNAPSHOT.jar` e `ArenaParkour-0.3.0-SNAPSHOT.jar` de seus respectivos `build/libs` em `server/plugins`, mantendo apenas um JAR de cada plugin. Inicie pelo script existente. Comandos administrativos:

```text
/palace generate
/palace status
/palace verify
```

O planejamento ocorre fora da thread principal; os blocos são aplicados na thread correta, com chunks carregados assincronamente, um ticket temporário por vez e orçamento de 3 ms/2.500 operações por tick. Marcadores `palace-v1.pending` e `palace-v1.complete` protegem a geração. Se um mundo homônimo já existir sem marcador de propriedade do gerador, a escrita é recusada. Falhas podem ser retomadas reaplicando a geração incompleta. Não há comando de sobrescrita do mundo concluído.

## Inspeção visual e física

Não houve captura real de Minecraft nem execução física dos saltos nesta entrega. Os PNGs de `build/palace-previews` são projeções diagnósticas dos blocos, com materiais em cores aproximadas e corte no teto: **não são screenshots do Minecraft**. Não demonstram iluminação real, transparência ou dificuldade jogada.

Para ver todas as câmaras:

```text
/palace view easy 1
/palace view easy 2
/palace view easy 3
/palace view medium 1
/palace view medium 2
/palace view medium 3
/palace view hard 1
/palace view hard 2
/palace view hard 3
/palace view impossible 1
/palace view impossible 2
/palace view impossible 3
```

Inspecione sem shaders: entradas e placa a partir do centro; teto olhando para cima; cada percurso da galeria. Para a fachada, OP pode usar Criativo e voar pelo perímetro; saia do treino antes. Em seguida use `/palace start` em cada dificuldade e confirme todos os saltos, quedas, três checkpoints e chegada. Tente alcançar o final sem os pousos intermediários: a conclusão deve ser recusada. Teste `/palace leave` e uma desconexão para conferir restauração do inventário e modo de jogo.

Os testes automatizados verificam direção das quatro alas, 12 finais de etapa, 88 apoios, espaço livre de cabeça, limites de distância/elevação, validade dos YAMLs e os testes existentes de corrida. Isso não substitui o playtest nem uma aprovação estética de qualidade comercial.

## Arquivos e recuperação

- `palace/PalaceArchitecture.java`: arquitetura e os 12 cenários;
- `palace/PalaceRoute.java`: traçado e cadastro das arenas;
- `palace/PalaceBlocks.java`, `PalaceBuilder.java`: voxelização e geração;
- `palace/PalaceService.java`: treino, records, comandos e displays;
- `ParkourAdapter.java`: manutenção da integração oficial, com limites de percurso para o palácio;
- `PersistentSnapshotStore.java`: agora preserva também os estados de voo;
- `src/test/.../palace`: validação geométrica e cortes de inspeção.

Backup anterior: `cassino-lobby/backups/parkour-before-palace-20261013-133633/`, contendo fontes anteriores do Parkour, JAR e dados do ArenaCore instalado e configuração anterior do lobby. Os mundos do lobby continuam intactos. Para reverter a implantação, encerre o servidor, remova somente o novo JAR ArenaParkour e recoloque o `ArenaCore-deployed.jar` e a configuração de lobby do backup. Preserve `palace-snapshots` até todos os inventários terem sido restaurados. O mundo do palácio pode continuar no disco sem ser carregado.

## Evidências da entrega — 13/10/2026

- Oito testes do módulo passaram: dois de configuração de arena, quatro de RaceTracker, um de geometria completa e um de projeções de inspeção.
- Primeira geração inspecionada em 14 projeções (12 câmaras, átrio e exterior). Corrigidas as janelas antes encobertas pelo lado externo e adicionadas nervuras superiores e alturas diferentes na cobertura.
- A primeira geração foi movida, com o servidor desligado, para `backups/parkour-before-palace-20261013-133633/first-generated-world`, preservando-a integralmente antes da revisão final.
- A revisão final gerou **282.871 posições de bloco** em Paper 1.21.11. Log às 13:53:38 de São Paulo: `PALACE_READY`.
- Às 13:53:47, `/palace verify` confirmou 88 apoios com espaço livre, quatro pistas e 12 finais de etapa; `/palace status` retornou `ready=true`.
- Após reinício limpo, às 13:55:00, o mundo e as quatro arenas foram reabertos; `ready=true` e as 88 verificações passaram novamente. O servidor foi deixado online em `127.0.0.1:25565`.
- O comando de geração duplicada foi recusado após a conclusão. Os três plugins carregaram juntos, e os quatro arquivos de arena foram persistidos.
- Não foram simuladas vitórias nem lançados pontos em campeonatos existentes. Saltos, inventário durante treino e partidas completas ainda precisam do teste interativo no cliente conforme o roteiro acima.

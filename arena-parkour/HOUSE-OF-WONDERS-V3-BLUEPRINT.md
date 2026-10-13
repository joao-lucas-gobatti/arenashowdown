# House of Wonders V3 — Blueprint de arquitetura e level design

Status: planta aprovada para implementação programática isolada. Data: 2026-10-13.

## Preservação e isolamento

- Backup imutável pré-V3: `cassino-lobby/backups/parkour-v2-pre-house-of-wonders-v3-20261013.zip`.
- SHA-256: `EF8C373F2292F2BA3CD21A7FAC946B691089093B213703BC812849A859B0F41C`.
- A V3 não altera `grand_dice_palace`, `palace-v1.*`, `palace-*` nem `palace-records.yml`.
- Novo mundo: `house_of_wonders_v3`.
- Novo package: `com.arenashowdown.parkour.wonder`.
- Novo comando: `/wonder`; novos IDs competitivos `wonder-easy`, `wonder-medium`, `wonder-hard`, `wonder-impossible` continuam servidos pelo adaptador `parkour` já registrado no ArenaCore.
- Marcadores versionados: `wonder-v3.pending` e `wonder-v3.complete`; o manifesto inclui schema, mundo, blueprint e contagem de blocos.

## Planta geral

Envelope construído: X/Z `-224..224` (449 × 449), Y `58..184` (127 blocos). Piso de referência Y=64.

| Zona | Coordenadas locais/globais | Volume principal | Função |
|---|---:|---:|---|
| Grand Wonder Atrium | X/Z -38..38 | 77×77×91 | hub, fonte, jardins, mezaninos, lustre e dado anelar |
| Fácil — norte | u -44..44, v 42..216 | 89×175×92 | Candy Casino |
| Médio — leste | u -44..44, v 42..216 | 89×175×108 | Celestial Card Palace |
| Difícil — oeste | u -44..44, v 42..216 | 89×175×116 | Crimson Clockwork |
| Impossível — sul | u -44..44, v 42..216 | 89×175×121 | Royal Dice Dimension |

Orientação preservada: norte Fácil, leste Médio, oeste Difícil, sul Impossível. Cada ala usa coordenadas locais `u` (lateral), `v` (distância do centro) e `h` (altura acima de Y=64), transformadas cardinalmente para X/Z.

## Capítulos, paletas e landmarks

| Ala | Fase / intervalo v | Paleta material | Landmark principal | Peças secundárias | Mecânicas dominantes | Transições |
|---|---|---|---|---|---|---:|
| Fácil | 1 Confetti Garden / 48..94 | cherry, calcite, mint copper, yellow/blue | árvore-fonte de fichas | flores monumentais, pontes curvas | fichas largas, pequenas curvas, escultura | 42 |
| Fácil | 2 Giant Toy Room / 104..150 | peach, cream, pastel yellow/blue | torre de dados-brinquedo | relógio, blocos e engrenagens | ascensão em espiral, travessia e descida | 42 |
| Fácil | 3 Rainbow Ballroom / 160..206 | pink, light blue, mint, champagne | teto de vidro e lustre arco-íris | balcões, cortinas, varandas | galerias, lustres, mezanino final | 42 |
| Médio | 1 Floating Card Kingdom / 48..94 | sky, turquoise, lilac, ivory, gold | castelo de cartas suspenso | nuvens, ilhas e vitrais | diagonais, contorno de torres | 42 |
| Médio | 2 Sky Clockwork / 104..150 | cyan, blue, gold, ivory | mostrador monumental | 7 engrenagens, ponteiros e torre | anéis, dentes, espiral vertical | 42 |
| Médio | 3 Celestial Mirror Hall / 160..206 | pale blue, amethyst, quartz, gold | prisma-espelho axial | cristais, arcos impossíveis | reversões, alturas e precisão moderada | 42 |
| Difícil | 1 Broken Theatre / 48..94 | wine, navy, copper, amber, cream | palco quebrado de 40 blocos | balcões, passarelas e cortinas | vigas, balcões, mudanças de direção | 42 |
| Difícil | 2 Mechanical Wonder / 104..150 | oxidized copper, crimson, dark blue | máquina de roleta vertical | tubulações, rodas e elevadores | dentes irregulares, travessia vertical | 42 |
| Difícil | 3 Grand Showdown / 160..206 | burgundy, amber, navy, cream | anfiteatro interno | estátuas, arcos e rigging | arena multinível e ascensão final | 42 |
| Impossível | 1 Fractured Reality / 48..94 | deep purple, electric blue, cyan, silver | dado colossal fraturado | anéis e torres quebradas | precisão, diagonais e superfícies estreitas | 42 |
| Impossível | 2 Infinite Palace / 104..150 | purple, magenta, cyan, white | escadaria impossível | torres, vitrais e arcos fragmentados | vários andares e reversões avançadas | 42 |
| Impossível | 3 Crown of Dice / 160..216 | royal purple, electric blue, magenta, silver | coroa de 56 blocos e dado suspenso | anéis, raios e espiras | ascensão técnica até Y≈174 | 42 |

Total: **12 fases, 504 transições significativas**. Cada fase possui 43 pousos contando a entrada e 42 transições. O layout não é uma linha: usa serpentinas arquitetônicas, órbitas, perímetros, espirais, zigue-zagues, galerias e ascensões.

## Curvas de dificuldade e física

Perfis de plataforma (largura típica / vão útil máximo / subida comum):

- Fácil: 3×3 ou 2×3 / 2.4 blocos / +1; áreas de preparação generosas.
- Médio: 2×2, slabs e bordas de 2 / 2.8 / +1; diagonais e reversões moderadas.
- Difícil: 1×2, vigas, paredes e dentes / 3.0 / +1; sequências longas e pousos menores.
- Impossível: 1×1, trapdoors, end rods estruturais e bordas / 3.15 / +1; mudanças diagonais e ritmo avançado.

A validação estática mede distância entre as bordas das superfícies, subida, queda, headroom, suporte, ordem e bounds. Slime, honey e ladders são usados somente em módulos explicitamente marcados; não tornam automaticamente um salto válido. A validação matemática não substitui playtest humano.

## Checkpoints e pontos importantes

- Hub/spawn: `(0.5, 65, 8.5)`, olhando para o dado central.
- Centro cenográfico: `(0.5, 86, 0.5)`.
- Entrada de cada ala: local `(0,65,42)`.
- Início oficial: primeiro pouso local de cada ala, aproximadamente `v=48`.
- Checkpoints: após transições 14, 28 e 42 de cada fase — 9 por ala, 36 no complexo.
- Conclusão de fase também é checkpoint; a conclusão da fase 3 é o finish competitivo.
- Varandas de conclusão: local `(0, altura final+1, 212)`; Crown of Dice termina no topo da coroa.
- Queda segura: abaixo de Y=60 ou fora do volume da rota retorna ao checkpoint salvo.
- Lobby de arena: `(0.5,65,8.5)`.

Transformação local para global:

- Fácil/norte: `(x,z)=(-u,-v)`, yaw 180°.
- Médio/leste: `(x,z)=(v,u)`, yaw -90°.
- Difícil/oeste: `(x,z)=(-v,-u)`, yaw 90°.
- Impossível/sul: `(x,z)=(u,v)`, yaw 0°.

As coordenadas exatas de cada pouso/checkpoint são derivadas deterministicamente pelo blueprint e serão exportadas pelo comando `/wonder report` e no relatório gerado.

## Composição do átrio

O Grand Wonder Atrium combina abóbada facetada marfim/azul-petróleo, quatro galerias assimétricas, jardins suspensos, fonte de coral e menta, escadarias curvas cenográficas, vitrais profundos, quatro portais de silhuetas diferentes, lustre de três anéis e um dado oco chanfrado com três órbitas. O piso usa roseta radial — nenhum padrão xadrez. Cada portal antecipa a arquitetura da ala: arco floral, castelo de cartas, boca de palco mecânica e fratura anelar.

## Arquitetura de software

1. `WonderRoute`: blueprint determinístico, 12 layouts, perfis e metadados de obstáculos.
2. `WonderBlocks`: plano voxel last-write-wins e leitura para validação; não acessa Bukkit world.
3. `WonderComponents`: esculturas especializadas (dados chanfrados, cartas, engrenagens, pontes, escadas curvas, lustres, arcos, jardins, relógios, cristais, coroa).
4. `WonderArchitecture`: composição única do átrio e de cada um dos 12 capítulos; integra o percurso ao cenário.
5. `WonderBuilder`: planejamento assíncrono e aplicação na main thread com chunks carregados, tickets temporários, limite de operações/tempo e física desativada.
6. `WonderService`: comandos, treino, checkpoints, queda segura, recordes, teleporte e instalação das quatro arenas no `ArenaStore`.
7. Validação JUnit: escala, contagens, unicidade de layouts, envelope físico, headroom, suporte, checkpoints, paletas e bounds.

## Fases de implantação

1. Gerar em ambiente de teste com `/wonder generate`.
2. Acompanhar `/wonder status` e aguardar `WONDER_V3_READY`.
3. Executar `/wonder verify` e `/wonder report`.
4. Inspecionar `/wonder view easy 1` e realizar playtest completo com `/wonder start easy`.
5. Repetir por ala; testar corrida oficial via ArenaCore com arena `wonder-easy` etc.
6. Somente após aceite visual, alterar portais/configuração de produção. O Palace permanece disponível para rollback.

## Referências estudadas

- [Parkour Spiral 3 — Hielke](https://hielkemaps.com/maps/parkour-spiral-3)
- [Aetherfall — Varuna](https://varunabuilds.com/aetherfall)
- [Nova Nexus — Varuna](https://varunabuilds.com/nova-nexus)

Princípios foram reinterpretados; nenhum personagem, asset ou construção foi copiado. Conteúdo das fontes foi reformulado para conformidade com licenciamento.

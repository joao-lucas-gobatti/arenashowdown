# Validação executada em 14/10/2026

Ambiente: Windows, Java Microsoft OpenJDK 21.0.7, Gradle 9.2.1, API Paper 1.21.11.
Todos os testes e arquivos TNT Run foram criados nesta reconstrução; nenhum resultado anterior foi reaproveitado.

## Build final

Na raiz `C:\Users\Rafael\arenashowdown`:

```powershell
$env:GRADLE_USER_HOME = 'C:\Users\Rafael\.gradle'
$env:DEBUG = ''
.\gradlew.bat :arena-tntrun:clean :arena-tntrun:build --init-script arena-tntrun/isolated-build.gradle --project-cache-dir arena-tntrun/.gradle-isolated --offline
```

Resultado final: **BUILD SUCCESSFUL**, 12s, 15 tarefas: 10 executadas e 5 atualizadas.
`clean`, compilação, testes, JAR e ShadowJar concluíram. Nenhum teste foi ignorado.
Avisos de depreciação relativos a APIs Bukkit ainda disponíveis nesta versão; não houve erro de compilação.

| Suíte nova | Testes | Falhas/erros |
| --- | ---: | ---: |
| SurvivalRoundTest | 7 | 0 |
| ArenaAndPersistenceTest | 8 | 0 |
| AdapterIntegrationTest | 18 | 0 |
| CommandPermissionsTest | 3 | 0 |
| CoreBridgeTest | 1 | 0 |
| **Total** | **37** | **0** |

Relatórios completos: `arena-tntrun/build/reports/tests/test/index.html` e XMLs em
`arena-tntrun/build/test-results/test/`. São artefatos locais ignorados pelo Git.

Cobertura verificada: contagem completa, início antecipado recusado, passos estacionários,
atraso exato, piso e suporte, camadas inferiores, queda, vitória, eliminação simultânea,
timeout e ordem determinística, equipe no resultado, cancelamento durante preparação e callbacks
atrasados, desconexão/reconexão, espectadores, dano/quebra/teleporte, permissões, rascunhos,
reload transacional, arquivos truncados, recuperação por instâncias novas, mundo indisponível,
outbox e resultado já finalizado.

`CoreBridgeTest` usa o **Core real** (`ArenaOrchestrationService`, `MatchManager`, repositórios JDBC e
política de pontuação), com banco SQLite novo em `@TempDir` e jogadores/Bukkit simulados.
Uma partida `tntrun` completa registra posições, equipes, 10/7 pontos e progresso da rodada.
Reenvio reconhecido mantém exatamente dois eventos de pontuação. Reabrir esse banco mantém
o resultado e o ranking. Nenhum banco existente foi usado.

## Verificação no Paper real

Servidor novo: `C:\Users\Rafael\arenashowdown\arena-tntrun\.test-server`.
Bind exclusivo em `127.0.0.1:25679`, Paper **1.21.11-132-c5eb079**, Java 21.
Somente JARs/cache/bibliotecas foram copiados de um servidor local; mundos, mapas, playerdata,
configurações de plugins e bancos existentes não foram copiados.
O Core foi recompilado com saídas isoladas em `.work/core-build` para esse teste.

Plugins usados: ArenaCore, ArenaTNTRun e ArenaTNTRunVerification (exclusivo de teste).
Execução com console interativo:

```text
java -Darenatntrun.isolated=true -Dterminal.jline=false -Dterminal.ansi=false -Xms512M -Xmx1G -jar paper.jar --nogui
tntrunverify
tntrun list
tntrun validate teste
tntrun status
stop
```

Resultado do JAR final às 22:14:12, horário de São Paulo:

```text
[ArenaTNTRun] ArenaTNTRun registered as tntrun; arenas=1
[ArenaTNTRunVerification] TNTRUN_PAPER_VERIFICATION_OK
Arenas: teste
YAML válido: teste; mundo carregado=true
IDLE
Stopping the server
```

O verificador construiu pisos somente no mundo novo do teste, removeu areia/TNT sem física e restaurou
BlockData com uma nova instância de journal. Usou serialização Paper real de espada encantada,
nome customizado, escudo no offhand e item no cursor; validou restauração de efeitos, absorção,
saúde acima de 20 com HEALTH_BOOST, objetivos/scores/equipes do scoreboard e estado escalar.
O objeto jogador nessa verificação é simulado; não se afirma uma partida com clientes humanos.
O verificador recusa ambiente fora de `.test-server` ou sem a propriedade de isolamento.

Log local: `.test-server/logs/latest.log`. Marcador: `.test-server/plugins/ArenaTNTRunVerification/verification.ok`.
Trecho registrado em [paper-verification.log](paper-verification.log). Servidor encerrado por `stop`.

## Artefato entregue

```text
C:\Users\Rafael\arenashowdown\arena-tntrun\build\libs\ArenaTNTRun-0.1.0-SNAPSHOT.jar
SHA256 FA6633E7587AE2B8E34F0557393D3A266BD2881328C00CB0248C87CFFD032369
```

A inspeção do JAR confirmou ausência de classes ArenaAPI, ArenaCore, ArenaParkour e do verificador.
O verificador é um segundo JAR separado, que não deve ser instalado em produção.

Limites: uma partida por adapter; operações de snapshot/restore síncronas; recuperação de scoreboard
avançado limitada conforme o README; sem clientes humanos, teste de carga, anticheat ou convivência
com plugins que editam estado diretamente. Recuperação após interrupção foi testada por reinstanciação
sem cleanup e por persistência real em disco, não por desligamento físico abrupto da máquina.

Referências de API consultadas: [Player Paper 1.21.11](https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/Player.html)
e [ItemStack Paper 1.21.11](https://jd.papermc.io/paper/1.21.11/org/bukkit/inventory/ItemStack.html).

# Relatório de testes — 13/10/2026

## Resultado

- `gradlew build --no-parallel`: aprovado com Gradle 9.2.1 e Temurin 21.0.12.1.
- 5 testes JUnit aprovados: ciclo de estados, bloqueio de inscrição, idempotência transacional de pontuação, recuperação de backup e rejeição de backup inválido.
- JAR sombreado gerado com SQLite e HikariCP: `ArenaCore-0.1.0-SNAPSHOT.jar`.
- Paper 1.21.11 build 132 iniciado em `127.0.0.1:25565`.
- ArenaCore 0.1.0-SNAPSHOT carregado e habilitado com SQLite.
- Esquema SQLite criado e reaberto em reinício posterior.
- Comando `arena list` executado no console e recuperou o campeonato persistido `smoke`.
- `scripts/start-server.ps1` iniciou o servidor e `scripts/stop-server.ps1` realizou desligamento limpo.
- Logs de desligamento confirmaram encerramento normal do pool Hikari e salvamento dos mundos.

## Ocorrências corrigidas

- O caminho Windows contém caracteres acentuados (`João` e `Área`). O worker do Gradle gerava classpath corrompido; `file.encoding=windows-1252` foi aplicado apenas ao daemon do projeto.
- JUnit 6 requer o launcher da plataforma no runtime; a dependência foi declarada explicitamente.
- A versão do plugin era exibida literalmente como `${version}`; o manifesto foi corrigido e o boot final confirmou `0.1.0-SNAPSHOT`.
- O primeiro canal de encerramento mantinha o arquivo aberto no OneDrive; o script passou a fazer polling sem lock e foi retestado.

## Alteração SQLite-only

- PostgreSQL deixou de ser requisito e seu driver foi removido.
- Backup e recuperação são cobertos por testes com diretórios temporários; nenhum banco de desenvolvimento existente é apagado ou substituído.
- Backups inválidos são rejeitados pelo teste de integridade do SQLite.
- Boot real confirmou criação e validação assíncrona do backup `arena-20261013-133209-171.db` sem alterar o banco original.
- O campeonato `smoke` permaneceu consultável após a atualização e novo reinício.

## Limites do teste

- Não houve conexão de um cliente Minecraft real; inscrição de jogador foi coberta por teste unitário da camada de serviço.
- O Paper avisa que 1.21.11 não é a geração mais nova, mas também confirma que o build 132 é o mais recente desta versão escolhida.

## Entrega 2 — campeonato e orquestração

Data: 13/10/2026.

### Testes automatizados

- 8 testes JUnit executados, 0 falhas e 0 erros.
- Campeonato completo por equipes executado com `FakeMinigame`.
- Cobertos: criação, inscrição UUID, equipes, atribuição, pausa/retomada, sequência de minigame, estados de partida, resultado oficial, empate, ranking individual/equipe, desconexão, reconexão, abandono sem reconexão, resultado duplicado, reinício de rodada e reabertura do SQLite.
- Backup, restauração e rejeição de backup inválido continuam cobertos.

### Teste real no Paper

- Uma instância já estava ativa em `127.0.0.1:25565`; ela não foi encerrada nem alterada durante o teste.
- Foi criado um servidor descartável e ignorado pelo Git em `server-test-delivery2`, usando `127.0.0.1:25566`.
- Paper 1.21.11 build 132 carregou ArenaCore `0.2.0-SNAPSHOT` sem erro de plugin.
- Campeonato `d2`, modo `TEAMS`, criado com sequência `parkour`.
- Alice e Bob foram inscritos por UUID e atribuídos às equipes Red e Blue.
- A partida `d2-r1` percorreu `WAITING → PREPARING → COUNTDOWN → RUNNING → FINISHED`.
- O comando enviou pontos 99/88; a política YAML recalculou corretamente para 10/7.
- Rankings retornaram Alice 10, Bob 7, Red 10/1 vitória e Blue 7/0 vitórias.
- Reenvio do mesmo resultado foi rejeitado sem duplicar pontos.
- `d2-r1-retry` foi criado para a mesma rodada sem reaplicar o resultado anterior.
- Campeonato finalizado com campeão `red`.
- Após reinício completo do Paper, campeão, rodada, partidas e rankings permaneceram disponíveis.
- Dois backups SQLite automáticos foram criados e validados durante os boots.
- Desligamentos concluíram com encerramento limpo do Hikari e salvamento dos mundos.

### Compatibilidade dos dados anteriores

- O banco original foi consultado sem escrita enquanto a instância principal permanecia ativa.
- O campeonato anterior foi encontrado como `smoke | Copa Smoke | PREPARING`.
- As migrações da Entrega 2 são exclusivamente aditivas; tabelas e eventos anteriores não são removidos.

### Limitações não validadas em cliente real

- Nenhum jogador Minecraft real conectou ao servidor de teste; snapshots Bukkit de inventário/localização e eventos `PlayerJoin/PlayerQuit` não foram exercitados end-to-end.
- Nenhum minigame externo real registrou `MinigameAdapter`; o contrato foi validado com implementação simulada.
- O retorno provisório ao lobby usa o spawn do mundo atual.

## Entrega 3 — ArenaParkour

Data: 13/10/2026.

### Testes automatizados

- Build completo com Temurin 21.0.12.1 e Gradle 9.2.1.
- Todos os testes anteriores do ArenaCore permaneceram aprovados.
- Seis testes do ArenaParkour aprovados: validação de arena, região 3D, checkpoints sequenciais, bloqueio de chegada antecipada, classificação, empate e timeout.
- A idempotência de resultados continua coberta pelos testes de integração do ArenaCore.

### Boot real isolado

- Paper 1.21.11 build 132 iniciado em `127.0.0.1:25567`, em `server-test-delivery3`.
- ArenaCore 0.2.0-SNAPSHOT e ArenaParkour 0.3.0-SNAPSHOT foram habilitados juntos.
- O primeiro boot revelou uma cópia da API duplicada no JAR; o empacotamento foi corrigido para `compileOnly`, e o segundo boot confirmou o adaptador.
- `/plugins` mostrou os dois plugins ativos; `/arena help` confirmou os novos comandos.
- SQLite e backup iniciaram normalmente; o desligamento foi limpo.
- O servidor principal na porta 25565 e os mundos `cassino_lobby` não foram alterados nem encerrados.

### Não validado

- Não houve dois clientes reais conectados; teleporte, scoreboard, sons, partículas e corrida completa precisam de teste manual.
- `buildtest` e seleção de regiões exigem jogador e não foram executados pelo console.
- A recuperação persistente de snapshot foi implementada, mas uma queda abrupta com cliente real ainda não foi reproduzida.

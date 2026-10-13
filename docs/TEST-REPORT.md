# Relatório de testes — 13/10/2026

## Resultado

- `gradlew build --no-parallel`: aprovado com Gradle 9.2.1 e Temurin 21.0.12.1.
- 3 testes JUnit aprovados: ciclo de estados, bloqueio de inscrição e idempotência de pontuação.
- JAR sombreado gerado com SQLite, PostgreSQL e HikariCP: `ArenaCore-0.1.0-SNAPSHOT.jar`.
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

## Limites do teste

- PostgreSQL está configurável e o driver foi empacotado, mas não foi testado porque não há instância PostgreSQL local autorizada/configurada.
- Não houve conexão de um cliente Minecraft real; inscrição de jogador foi coberta por teste unitário da camada de serviço.
- O Paper avisa que 1.21.11 não é a geração mais nova, mas também confirma que o build 132 é o mais recente desta versão escolhida.

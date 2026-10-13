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

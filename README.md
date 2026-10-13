# Arena Showdown

Infraestrutura central de competições para Paper 1.21.11. Esta primeira entrega contém o contrato público `arena-api` e o plugin `arena-core`; nenhum minigame foi implementado ainda.

## Requisitos

- Windows 10/11 x64
- JDK 21 (o ambiente local usa Temurin 21 em `.tools/jdk-21`)
- Paper 1.21.11 build 132 em `server/paper-1.21.11-132.jar`

O Gradle Wrapper fixa a versão do build, portanto não é necessário instalar Gradle globalmente.

## Compilar e testar

```powershell
.\scripts\build.ps1
```

O artefato final é `arena-core/build/libs/ArenaCore-0.1.0-SNAPSHOT.jar`. Copie-o para `server/plugins/` antes de iniciar o servidor.

## Executar localmente

```powershell
New-Item -ItemType Directory -Force server\plugins
Copy-Item arena-core\build\libs\ArenaCore-0.1.0-SNAPSHOT.jar server\plugins\
.\scripts\start-server.ps1
```

O servidor escuta somente em `127.0.0.1:25565`. Em outro PowerShell, encerre de forma limpa com `./scripts/stop-server.ps1`.

## Comandos

- `/arena create <id> <nome>`
- `/arena prepare|start|finish|cancel <id>`
- `/arena list`
- `/arena join <id> [equipe]`
- `/arena score <id> <jogador-ou-uuid> <pontos> <origem> <evento-id>`
- `/arena ranking <id>`

Operações administrativas exigem `arena.admin` (OP por padrão). Participação e consulta usam `arena.play`.

## Persistência e API

SQLite é o padrão de desenvolvimento. PostgreSQL pode ser selecionado em `plugins/ArenaCore/config.yml`. Todas as operações de banco são executadas fora da thread principal. Cada evento de pontuação possui `event-id` único, impedindo crédito duplicado mesmo após reinício. Plugins de minigame obtêm `ArenaApi` pelo `ServicesManager` do Bukkit.

## Limitações atuais

- Equipes são registradas como identificadores dos participantes; cadastro e ranking agregado de equipes ainda não têm comandos próprios.
- Lobby e scoreboard têm ponto de integração via API, mas ainda não possuem apresentação visual.
- Migrações usam criação idempotente de esquema; versionamento formal de migrations será necessário antes de produção.
- Velocity e os oito minigames estão deliberadamente fora desta primeira etapa.

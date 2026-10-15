# Auditoria de preservação

Pasta: `C:\Users\Rafael\arenashowdown`. Branch inicial e final: `dev/rafael`.
HEAD de referência antes desta reconstrução: `ff3b381`.
Não foi encontrado `AGENTS.md` na árvore do repositório nem nos diretórios ancestrais consultados.

O status original completo, com os arquivos não rastreados expandidos, está em
[initial-status.txt](initial-status.txt). Antes da edição, foram registrados SHA256 de **2.263 arquivos**
fora de `arena-tntrun`, incluindo os arquivos modificados/não rastreados iniciais.
Manifesto local: `arena-tntrun/.work/baseline.json`; referência: `.work/initial-head.txt`.
Esses arquivos de trabalho são ignorados pelo Git.

A comparação após implementação, builds e servidor isolado confirmou **2.263 hashes idênticos**.
Isso inclui os artefatos previamente modificados de Gradle, Core, Parkour e os arquivos não rastreados
do Lobby e `-files --others --exclude-standard`. O status externo ao módulo permaneceu igual ao inicial.

O diff de commits é conferido com:

```powershell
git diff --name-only ff3b381 HEAD
git log --oneline ff3b381..HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File arena-tntrun/tools/Verify-Preservation.ps1
```

Todos os caminhos do intervalo de commits começam com `arena-tntrun/`. Não há sobreposição com
os caminhos modificados/não rastreados do status inicial. `git diff --check -- arena-tntrun` passou.
Nenhum arquivo do build/JAR, servidor novo ou banco temporário foi adicionado aos commits.

Cada commit foi preparado com caminhos explícitos e com conferência de que o index continha
somente `arena-tntrun/`. Não foi usado `git add .`, reset/clean, force push, amend ou reescrita.
Não houve push. APIs compartilhadas, ArenaParkour e servidores/mundos/mapas/bancos existentes
não foram editados. Os únicos mundos e banco de servidor usados foram criados dentro do novo
`arena-tntrun/.test-server`; o servidor foi encerrado. Testes JDBC usaram `@TempDir` novo.

O build isolou o cache de projeto Gradle e as saídas de ArenaAPI/Core/raiz para não sobrescrever
os artefatos anteriores. O cache de dependências global foi usado normalmente pelo Gradle,
com autorização necessária ao acesso fora do sandbox. Escrita em `.git` para os commits locais
também foi autorizada; isso não alterou o conteúdo dos arquivos anteriores.

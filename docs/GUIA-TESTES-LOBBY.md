# Guia de testes do ArenaLobby 4.1

## Preparacao

Use um operador no Minecraft. Confirme que ArenaCore, ArenaParkour e ArenaLobby aparecem verdes em `/plugins`. Escolha um campeonato real com `/lobby setchampionship <id>`.

## Spawn e comandos

1. Fique no ponto exato do lobby, com piso solido e dois blocos livres.
2. Execute `/lobby setspawn`.
3. Mude de posicao e teste `/lobby`, `/l` e `/hub` separadamente.
4. Confirme coordenadas, yaw e pitch exatos apos cada retorno.
5. Reinicie o Paper e repita `/lobby` para validar persistencia.

Durante uma corrida oficial, `/lobby` deve recusar o teleporte. Isso impede fuga e preserva o snapshot do ArenaParkour.

## Hotbar e GUI

1. Entre no lobby. Os itens funcionais devem surgir apenas em slots vazios: informacoes (0), bussola (4) e ranking (8).
2. Clique com os dois botoes na bussola; ambos devem abrir uma GUI de 54 slots.
3. Confirme os oito jogos em duas linhas, Parkour disponivel somente com plugin/arena e os demais como indisponiveis.
4. Tente Shift+Click, arrastar e retirar itens: nenhuma manipulacao deve ser aceita.
5. Tente jogar fora ou guardar a bussola em um bau: a operacao deve ser bloqueada.
6. Saia do mundo do lobby e confirme que somente itens marcados pelo ArenaLobby foram removidos. Retorne e confirme que nao houve duplicacao.

O menu tambem abre explicitamente com `/lobby menu`; `/lobby` sozinho nunca deve abrir inventario.

## Scoreboard e TAB

No lobby, confira na lateral nome/estado do campeonato, rodada, classificacao real e proximo jogo. Pressione TAB e valide cabecalho, rodape, nickname original e prefixo de admin/equipe/espectador. Compare os pontos com `/arena ranking <id>`.

## Parkour e retorno

1. Configure um campeonato cuja proxima rodada seja `parkour` e uma arena valida.
2. Cadastre participantes pelo ArenaCore.
3. Na GUI, como operador, clique em **Iniciar proxima rodada**; alternativamente use `/arena next <id>`.
4. Confirme que o scoreboard do Parkour substitui o do lobby durante a corrida.
5. Termine a prova. O snapshot deve retornar cada participante ao lobby com inventario preservado.
6. Em ate cinco segundos, confira o scoreboard/TAB/ranking atualizados e a hotbar sem duplicacoes.

## Limites da validacao automatica

O boot Paper e as regras de estado foram testados automaticamente. Aparencia, clicks reais, orientacao do spawn e ciclo completo com clientes exigem esta verificacao dentro do Minecraft.

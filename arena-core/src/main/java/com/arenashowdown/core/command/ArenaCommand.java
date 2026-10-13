package com.arenashowdown.core.command;

import com.arenashowdown.api.model.ChampionshipState;
import com.arenashowdown.core.service.ArenaService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public final class ArenaCommand implements CommandExecutor, TabCompleter {
    private final Plugin plugin; private final ArenaService service; private final int rankingLimit;
    public ArenaCommand(Plugin plugin, ArenaService service, int rankingLimit) { this.plugin=plugin; this.service=service; this.rankingLimit=rankingLimit; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length==0 || args[0].equalsIgnoreCase("help")) { help(sender); return true; }
        try {
            switch(args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> { admin(sender); require(args,3,"/arena create <id> <nome>"); async(sender,service.createChampionship(args[1],String.join(" ",Arrays.copyOfRange(args,2,args.length))),c -> ok(sender,"Campeonato criado: "+c.id())); }
                case "prepare" -> transition(sender,args,ChampionshipState.PREPARING);
                case "start" -> transition(sender,args,ChampionshipState.RUNNING);
                case "finish" -> transition(sender,args,ChampionshipState.FINISHED);
                case "cancel" -> transition(sender,args,ChampionshipState.CANCELLED);
                case "list" -> async(sender,service.championships(),list -> { if(list.isEmpty()) info(sender,"Nenhum campeonato."); else list.forEach(c -> info(sender,c.id()+" — "+c.name()+" ["+c.state()+"]")); });
                case "join" -> { player(sender); require(args,2,"/arena join <campeonato> [equipe]"); Player p=(Player)sender; async(sender,service.registerPlayer(args[1],p.getUniqueId(),p.getName(),args.length>2?args[2]:null),v -> ok(sender,"Inscrição registrada.")); }
                case "score" -> score(sender,args);
                case "ranking" -> { require(args,2,"/arena ranking <campeonato>"); async(sender,service.ranking(args[1],rankingLimit),ranking -> { info(sender,"Ranking — "+args[1]); for(int i=0;i<ranking.size();i++) info(sender,(i+1)+". "+ranking.get(i).playerName()+" — "+ranking.get(i).points()+" pts"); }); }
                default -> help(sender);
            }
        } catch(IllegalArgumentException | IllegalStateException e) { error(sender,e.getMessage()); }
        return true;
    }

    private void transition(CommandSender sender,String[] args,ChampionshipState state) { admin(sender); require(args,2,"/arena "+args[0]+" <id>"); async(sender,service.transition(args[1],state),c -> ok(sender,"Estado atualizado: "+c.state())); }
    private void score(CommandSender sender,String[] args) { admin(sender); require(args,6,"/arena score <campeonato> <jogador> <pontos> <origem> <evento-id>"); UUID id; try { id=UUID.fromString(args[2]); } catch(IllegalArgumentException e) { Player p=Bukkit.getPlayerExact(args[2]); if(p==null) throw new IllegalArgumentException("Jogador offline: use o UUID"); id=p.getUniqueId(); } int points=Integer.parseInt(args[3]); async(sender,service.awardPoints(args[1],id,points,args[4],args[5]),inserted -> { if(inserted) ok(sender,"Pontuação registrada."); else error(sender,"Evento duplicado; nenhum ponto foi adicionado."); }); }
    private <T> void async(CommandSender sender,java.util.concurrent.CompletableFuture<T> future,Consumer<T> success) { future.whenComplete((value,failure) -> Bukkit.getScheduler().runTask(plugin,() -> { if(failure!=null) error(sender,root(failure).getMessage()); else success.accept(value); })); }
    private Throwable root(Throwable t) { while((t instanceof CompletionException) && t.getCause()!=null) t=t.getCause(); return t; }
    private void admin(CommandSender s) { if(!s.hasPermission("arena.admin")) throw new IllegalStateException("Sem permissão: arena.admin"); }
    private void player(CommandSender s) { if(!(s instanceof Player)) throw new IllegalStateException("Comando exclusivo para jogadores"); if(!s.hasPermission("arena.play")) throw new IllegalStateException("Sem permissão: arena.play"); }
    private void require(String[] a,int n,String usage) { if(a.length<n) throw new IllegalArgumentException("Uso: "+usage); }
    private void help(CommandSender s) { info(s,"/arena create|prepare|start|finish|cancel|list|join|score|ranking"); }
    private void ok(CommandSender s,String m) { s.sendMessage(Component.text(m,NamedTextColor.GREEN)); }
    private void info(CommandSender s,String m) { s.sendMessage(Component.text(m,NamedTextColor.GOLD)); }
    private void error(CommandSender s,String m) { s.sendMessage(Component.text(m==null?"Erro interno":m,NamedTextColor.RED)); }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args) { if(args.length==1) return List.of("help","list","join","ranking","create","prepare","start","finish","cancel","score").stream().filter(x->x.startsWith(args[0].toLowerCase())).toList(); return List.of(); }
}

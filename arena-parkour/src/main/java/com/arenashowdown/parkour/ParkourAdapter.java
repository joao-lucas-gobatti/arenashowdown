package com.arenashowdown.parkour;

import com.arenashowdown.api.OrchestrationApi;
import com.arenashowdown.api.minigame.*;
import com.arenashowdown.api.model.*;
import com.arenashowdown.parkour.arena.*;
import com.arenashowdown.parkour.player.PersistentSnapshotStore;
import com.arenashowdown.parkour.race.RaceTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public final class ParkourAdapter implements MinigameAdapter {
    private final Plugin plugin;private final OrchestrationApi api;private final ArenaStore arenas;private final PersistentSnapshotStore snapshots;
    private ArenaMatch match;private ParkourArena arena;private RaceTracker tracker;private MatchResult result;private final Set<UUID> participants=new LinkedHashSet<>();private final Map<UUID,String>teams=new HashMap<>();private final Map<UUID,ArenaPoint>lastPoints=new HashMap<>();private final Set<UUID>frozen=new HashSet<>();private BukkitTask ticker;private boolean ending;
    public ParkourAdapter(Plugin plugin,OrchestrationApi api,ArenaStore arenas,PersistentSnapshotStore snapshots){this.plugin=plugin;this.api=api;this.arenas=arenas;this.snapshots=snapshots;}
    @Override public String id(){return "parkour";}
    @Override public boolean supportsArena(String arenaId){return arenas.get(arenaId).map(a->a.validate().isEmpty()).orElse(false);}
    @Override public void prepare(MinigameContext context){ensureMain();resetRuntime();match=context.match();arena=arenas.get(match.arena()).orElseThrow(()->new IllegalStateException("Arena de Parkour inexistente: "+match.arena()));List<String>errors=arena.validate();if(!errors.isEmpty())throw new IllegalStateException("Arena incompleta: "+String.join(", ",errors));if(Bukkit.getWorld(arena.world())==null)throw new IllegalStateException("Mundo da arena nao carregado: "+arena.world());participants.addAll(match.participants());List<Player>online=new ArrayList<>();for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p==null){cancelPreparation("Participante offline: "+id);return;}if(java.nio.file.Files.exists(plugin.getDataFolder().toPath().resolve("palace-snapshots").resolve(id+".yml"))){cancelPreparation("Participante em treino: use /palace leave: "+p.getName());return;}online.add(p);}if(arena.spawns().size()<online.size()){cancelPreparation("Arena possui menos spawns que participantes");return;}api.participants(match.championshipId()).thenAccept(infos->infos.forEach(i->teams.put(i.playerId(),i.teamId())));CompletableFuture<?>[]writes=online.stream().map(snapshots::capture).toArray(CompletableFuture[]::new);CompletableFuture.allOf(writes).whenComplete((v,e)->Bukkit.getScheduler().runTask(plugin,()->{if(e!=null){cancelPreparation("Falha ao persistir snapshots: "+root(e).getMessage());return;}for(int i=0;i<online.size();i++){Player p=online.get(i);p.getInventory().clear();p.setGameMode(GameMode.ADVENTURE);p.setCollidable(false);p.teleport(location(arena.spawns().get(i)));lastPoints.put(p.getUniqueId(),arena.spawns().get(i));frozen.add(p.getUniqueId());}tracker=new RaceTracker(participants,arena.checkpoints().size(),plugin.getConfig().getLong("tie-threshold-ms",10));api.transitionMatch(match.id(),MatchState.COUNTDOWN,"ArenaParkour",null).whenComplete((m,f)->Bukkit.getScheduler().runTask(plugin,()->{if(f!=null)cancelPreparation(root(f).getMessage());else countdown(plugin.getConfig().getInt("countdown-seconds",5));}));}));}
    private void countdown(int seconds){if(match==null)return;final int[]left={seconds};ticker=Bukkit.getScheduler().runTaskTimer(plugin,()->{if(match==null){cancelTask();return;}if(left[0]>0){forPlayers(p->{p.showTitle(net.kyori.adventure.title.Title.title(Component.text(left[0],NamedTextColor.GOLD),Component.text("Prepare-se")));p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HAT,0.5f,1.2f);});left[0]--;return;}cancelTask();api.transitionMatch(match.id(),MatchState.RUNNING,"ArenaParkour",null).exceptionally(e->{Bukkit.getScheduler().runTask(plugin,()->cancelPreparation(root(e).getMessage()));return null;});},0,20);}
    @Override public void start(){ensureMain();if(match==null||tracker==null)return;frozen.clear();tracker.start(System.nanoTime());forPlayers(p->p.showTitle(net.kyori.adventure.title.Title.title(Component.text("VAI!",NamedTextColor.GREEN),Component.empty())));long started=System.nanoTime();ticker=Bukkit.getScheduler().runTaskTimer(plugin,()->tick(started),1,Math.max(1,plugin.getConfig().getLong("scoreboard-update-ticks",4)));}
    private void tick(long started){if(match==null||ending)return;long now=System.nanoTime();for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p==null)continue;if(p.getLocation().getY()<arena.fallHeight()){p.teleport(location(lastPoints.getOrDefault(id,arena.start())));p.sendActionBar(Component.text("Retorno ao ultimo checkpoint",NamedTextColor.RED));}if(Boolean.TRUE.equals(arena.rules().get("palace"))){validatePalacePosition(p);}updateProgress(p,now);updateBoard(p,now);}if(tracker.allFinished()||(now-started)/1_000_000_000L>=arena.timeLimitSeconds())complete();}
    private void updateProgress(Player p,long now){int expected=tracker.checkpoint(p.getUniqueId())+1;if(expected<=arena.checkpoints().size()&&arena.checkpoints().get(expected-1).contains(p.getWorld().getName(),p.getLocation().getX(),p.getLocation().getY(),p.getLocation().getZ())){if(tracker.checkpoint(p.getUniqueId(),expected)){lastPoints.put(p.getUniqueId(),point(p.getLocation()));p.sendActionBar(Component.text("Checkpoint "+expected+"/"+arena.checkpoints().size(),NamedTextColor.AQUA));p.playSound(p.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,0.6f,1.5f);p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,p.getLocation(),8,0.3,0.3,0.3,0);}}if(arena.finish().contains(p.getWorld().getName(),p.getLocation().getX(),p.getLocation().getY(),p.getLocation().getZ())&&tracker.finish(p.getUniqueId(),now)){int position=tracker.standings().stream().filter(x->x.playerId().equals(p.getUniqueId())).findFirst().orElseThrow().position();p.showTitle(net.kyori.adventure.title.Title.title(Component.text("CHEGADA!",NamedTextColor.GREEN),Component.text(position+" lugar - "+format(tracker.elapsed(p.getUniqueId(),now)))));p.setAllowFlight(true);p.setFlying(true);}}
    private void complete(){if(ending)return;ending=true;cancelTask();List<RaceTracker.Standing>s=tracker.standings();List<ResultEntry>entries=s.stream().map(x->new ResultEntry(x.playerId(),teams.get(x.playerId()),x.position(),0,x.position()==1)).toList();boolean tie=s.size()>1&&s.get(0).position()==s.get(1).position();result=new MatchResult(match.id()+"-parkour-result",match.id(),entries,tie,Instant.now());api.submitResult(match.id(),result,"ArenaParkour").whenComplete((ok,e)->Bukkit.getScheduler().runTask(plugin,()->{if(e!=null){plugin.getLogger().severe("Falha ao registrar resultado: "+root(e).getMessage());cancel("Falha ao registrar resultado");}}));}
    @Override public void finish(MatchResult official){ensureMain();result=official;forPlayers(p->p.sendMessage(Component.text("Parkour finalizado. Consulte /arena ranking "+match.championshipId(),NamedTextColor.GOLD)));restoreAll();}
    @Override public void cancel(String reason){ensureMain();forPlayers(p->p.sendMessage(Component.text("Parkour cancelado: "+reason,NamedTextColor.RED)));restoreAll();resetRuntime();}
    @Override public void reset(){ensureMain();cancelTask();}
    @Override public Optional<MatchResult> getResult(){return Optional.ofNullable(result);}
    @Override public Set<UUID> getParticipants(){return Set.copyOf(participants);}
    public boolean active(UUID id){return match!=null&&participants.contains(id);}
    public boolean frozen(UUID id){return frozen.contains(id);}
    public void playerQuit(Player p){if(active(p.getUniqueId()))api.match(match.id()).thenCompose(m->api.transitionMatch(m.id(),MatchState.CANCELLED,"ArenaParkour","Participante desconectado")).exceptionally(e->null);}
    public void recover(Player p){if(snapshots.exists(p.getUniqueId()))snapshots.restore(p).exceptionally(e->{plugin.getLogger().severe("Falha ao recuperar snapshot de "+p.getName()+": "+root(e).getMessage());return false;});}
    private void restoreAll(){for(UUID id:new ArrayList<>(participants)){Player p=Bukkit.getPlayer(id);if(p!=null){p.setAllowFlight(false);p.setFlying(false);p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());snapshots.restore(p);}}}
    private void cancelPreparation(String reason){plugin.getLogger().warning(reason);if(match!=null)api.transitionMatch(match.id(),MatchState.CANCELLED,"ArenaParkour",reason);restoreAll();resetRuntime();}
    private void resetRuntime(){cancelTask();match=null;arena=null;tracker=null;participants.clear();teams.clear();lastPoints.clear();frozen.clear();ending=false;}
    private void updateBoard(Player p,long now){Scoreboard board=p.getScoreboard();if(board==Bukkit.getScoreboardManager().getMainScoreboard()||board.getObjective("parkour")==null){board=Bukkit.getScoreboardManager().getNewScoreboard();Objective o=board.registerNewObjective("parkour",Criteria.DUMMY,Component.text("ARENA SHOWDOWN"));o.setDisplaySlot(DisplaySlot.SIDEBAR);p.setScoreboard(board);}Objective o=board.getObjective("parkour");for(String entry:new HashSet<>(board.getEntries()))board.resetScores(entry);o.getScore("PARKOUR RACE").setScore(5);o.getScore("Tempo: "+format(tracker.elapsed(p.getUniqueId(),now))).setScore(4);o.getScore("Checkpoint: "+tracker.checkpoint(p.getUniqueId())+"/"+arena.checkpoints().size()).setScore(3);int pos=1+(int)tracker.standings().stream().filter(x->x.nanos()<tracker.elapsed(p.getUniqueId(),now)).count();o.getScore("Posicao: "+pos).setScore(2);o.getScore("Jogadores: "+Bukkit.getOnlinePlayers().stream().filter(x->participants.contains(x.getUniqueId())).count()+"/"+participants.size()).setScore(1);}
    private void forPlayers(java.util.function.Consumer<Player>c){participants.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(c);}private void cancelTask(){if(ticker!=null){ticker.cancel();ticker=null;}}private Location location(ArenaPoint p){World w=Bukkit.getWorld(p.world());if(w==null)throw new IllegalStateException("Mundo nao carregado: "+p.world());return new Location(w,p.x(),p.y(),p.z(),p.yaw(),p.pitch());}private ArenaPoint point(Location l){return new ArenaPoint(l.getWorld().getName(),l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch());}private String format(long nanos){long ms=nanos/1_000_000;return String.format(Locale.ROOT,"%02d:%02d.%02d",ms/60000,(ms/1000)%60,(ms%1000)/10);}private void ensureMain(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Operacao Bukkit fora da thread principal");}private Throwable root(Throwable t){while(t instanceof CompletionException&&t.getCause()!=null)t=t.getCause();return t;}
    private void validatePalacePosition(Player p) {
        if (tracker == null || !p.isOnGround()) return;
        int passed=tracker.checkpoint(p.getUniqueId());
        if (passed>=arena.checkpoints().size())return;
        if(p.isFlying()||p.isGliding()||p.getGameMode()!=GameMode.ADVENTURE){
            p.setFlying(false);p.setAllowFlight(false);p.setGameMode(GameMode.ADVENTURE);
            p.teleport(location(lastPoints.getOrDefault(p.getUniqueId(),arena.start())));return;
        }
        var w=com.arenashowdown.parkour.palace.PalaceRoute.Wing.valueOf(String.valueOf(arena.rules().get("difficulty")));
        var expected=com.arenashowdown.parkour.palace.PalaceRoute.landings(w).get(passed+1);
        int lateral=-w.dz*p.getLocation().getBlockX()+w.dx*p.getLocation().getBlockZ();
        int forward=w.dx*p.getLocation().getBlockX()+w.dz*p.getLocation().getBlockZ();
        if(Math.abs(lateral)>5||forward>expected.v()+4)
            p.teleport(location(lastPoints.getOrDefault(p.getUniqueId(),arena.start())));
    }

}

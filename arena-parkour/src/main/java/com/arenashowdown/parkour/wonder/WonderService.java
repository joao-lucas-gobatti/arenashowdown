package com.arenashowdown.parkour.wonder;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import com.arenashowdown.parkour.ParkourAdapter;
import com.arenashowdown.parkour.arena.*;
import com.arenashowdown.parkour.player.PersistentSnapshotStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import static com.arenashowdown.parkour.wonder.WonderRoute.*;

/** Runtime for V3 generation, exploration and long-form training. Competitive races remain on ParkourAdapter. */
public final class WonderService implements Listener,CommandExecutor,TabCompleter,AutoCloseable {
    private final JavaPlugin plugin;private final ArenaStore arenas;private final ParkourAdapter adapter;
    private final WonderBuilder builder;private final PersistentSnapshotStore snapshots;
    private final Map<UUID,Run> runs=new HashMap<>();private final Set<UUID> pending=new HashSet<>();
    private final Map<UUID,Location> visitors=new HashMap<>();private java.util.function.Predicate<UUID> externalActive=id->false;
    private final ExecutorService io=Executors.newSingleThreadExecutor(Thread.ofPlatform().name("wonder-v3-io-",0).factory());
    private final YamlConfiguration records;private final Path recordsFile;private final BukkitTask ticker;
    private record ChunkPos(int x,int z){}
    private static final class Run {
        final Wing wing;long started=System.nanoTime();int next=1,saved,falls;boolean teleporting;
        Run(Wing wing){this.wing=wing;}
    }
    public WonderService(JavaPlugin plugin,ArenaStore arenas,ParkourAdapter adapter){
        this.plugin=plugin;this.arenas=arenas;this.adapter=adapter;
        snapshots=new PersistentSnapshotStore(plugin,plugin.getDataFolder().toPath().resolve("wonder-v3-snapshots"));
        recordsFile=plugin.getDataFolder().toPath().resolve("wonder-v3-records.yml");records=YamlConfiguration.loadConfiguration(recordsFile.toFile());
        builder=new WonderBuilder(plugin,this::install);
        PluginCommand command=Objects.requireNonNull(plugin.getCommand("wonder"),"Comando wonder ausente");command.setExecutor(this);command.setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this,plugin);ticker=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,2,2);
        if(builder.ready()){builder.world();Bukkit.getScheduler().runTask(plugin,this::install);}
        Bukkit.getOnlinePlayers().forEach(this::recover);
    }
    public boolean active(UUID id){return runs.containsKey(id)||pending.contains(id)||snapshots.exists(id);}
    public void externalActive(java.util.function.Predicate<UUID> predicate){externalActive=Objects.requireNonNull(predicate);}
    private void install(){
        Path ready=plugin.getDataFolder().toPath().resolve("wonder-v3.arenas-ready"),temporary=ready.resolveSibling("wonder-v3.arenas-ready.tmp");
        try{
            Files.deleteIfExists(ready);for(Wing wing:Wing.values())arenas.save(WonderRoute.arena(wing));
            for(Wing wing:Wing.values()){ParkourArena installed=arenas.get(wing.arenaId()).orElseThrow();if(!SCHEMA.equals(String.valueOf(installed.rules().get("schema")))||installed.checkpoints().size()!=126)throw new IllegalStateException("Arena V3 inconsistente: "+wing.arenaId());}
            Files.writeString(temporary,"blueprint="+WonderBuilder.BLUEPRINT+"\narenas=4\n",StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
            try{Files.move(temporary,ready,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,ready,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception error){try{Files.deleteIfExists(temporary);Files.deleteIfExists(ready);}catch(Exception suppressed){error.addSuppressed(suppressed);}throw new IllegalStateException("Não foi possível instalar atomicamente as arenas V3",error);}
    }
    private boolean inWorld(Player player){return player.getWorld().getName().equals(WORLD);}
    private Location hub(){return new Location(builder.world(),.5,FLOOR+1,8.5,180,0);}
    private void rememberVisitor(Player player){if(!inWorld(player))visitors.putIfAbsent(player.getUniqueId(),player.getLocation().clone());}
    private Location at(Wing wing,Landing node){ArenaPoint point=node.point(wing);return new Location(builder.world(),point.x(),point.y(),point.z(),point.yaw(),point.pitch());}
    private List<Landing> nodes(Wing wing){return WonderRoute.route(wing);}
    private void start(Player player,Wing wing){
        UUID id=player.getUniqueId();
        if(!builder.ready()){player.sendMessage("House of Wonders V3 ainda não foi gerada.");return;}
        if(adapter.active(id)||pending.contains(id)||externalActive.test(id)){player.sendMessage("Aguarde o encerramento da partida/operação atual.");return;}
        if(runs.containsKey(id)){player.sendMessage("Use /wonder leave antes de iniciar outra aventura.");return;}
        if(Files.exists(plugin.getDataFolder().toPath().resolve("palace-snapshots").resolve(id+".yml"))){player.sendMessage("Saia do treino Palace com /palace leave primeiro.");return;}
        if(snapshots.exists(id)){player.sendMessage("Use /wonder leave para restaurar seu estado antes de reiniciar.");return;}
        if(!player.getActivePotionEffects().isEmpty()){player.sendMessage("Aguarde efeitos de poção terminarem para uma tentativa justa.");return;}
        pending.add(id);snapshots.capture(player).whenComplete((unused,error)->{
            if(!plugin.isEnabled())return;Bukkit.getScheduler().runTask(plugin,()->{
                pending.remove(id);if(error!=null){player.sendMessage("Falha ao salvar seu estado; aventura não iniciada.");return;}if(!player.isOnline())return;
                Run run=new Run(wing);runs.put(id,run);preparePlayer(player);player.teleport(at(wing,nodes(wing).getFirst()));
                player.sendMessage(Component.text("THE HOUSE OF WONDERS — "+wing.title+". 3 capítulos, 126 transições e 9 checkpoints.",NamedTextColor.GOLD));
            });
        });
    }
    private void preparePlayer(Player player){player.getInventory().clear();player.setGameMode(GameMode.ADVENTURE);player.setFlying(false);player.setAllowFlight(false);player.setFoodLevel(20);player.setFallDistance(0);}
    private void tick(){
        for(var entry:new ArrayList<>(runs.entrySet())){
            Player player=Bukkit.getPlayer(entry.getKey());if(player==null)continue;Run run=entry.getValue();
            if(adapter.active(player.getUniqueId())){runs.remove(player.getUniqueId());continue;}
            if(!inWorld(player)){leave(player,false);continue;}if(run.teleporting)continue;
            if(player.isFlying()||player.isGliding()||player.getGameMode()!=GameMode.ADVENTURE){leave(player,true);player.sendMessage("Tentativa encerrada: voo ou modo de jogo alterado.");continue;}
            List<Landing> route=nodes(run.wing);Location location=player.getLocation();
            if(location.getY()<57||Math.abs(location.getBlockX())>224||Math.abs(location.getBlockZ())>224){reset(player,run);continue;}
            Landing expected=route.get(run.next);
            if(player.isOnGround()&&expected.region(run.wing).contains(WORLD,location.getX(),location.getY(),location.getZ())){
                run.next++;
                if(expected.checkpoint()){
                    run.saved=run.next-1;player.playSound(location,Sound.ENTITY_EXPERIENCE_ORB_PICKUP,.7f,1.35f);
                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,location,18,.5,.4,.5,0);
                    player.sendMessage(Component.text("Checkpoint — "+stageName(run.wing,expected.stage())+" "+expected.transition()+"/42",NamedTextColor.AQUA));
                }
                if(run.next==route.size()){finish(player,run);continue;}
                expected=route.get(run.next);
            }
            player.sendActionBar(Component.text(String.format(Locale.ROOT,"%s | %s | %d/42 | CP %d/9 | %.2fs | quedas %d",run.wing.name(),stageName(run.wing,expected.stage()),expected.transition(),checkpointOrdinal(route,run.saved),(System.nanoTime()-run.started)/1e9,run.falls),NamedTextColor.GOLD));
        }
    }
    private int checkpointOrdinal(List<Landing> route,int saved){int result=0;for(int i=1;i<=saved&&i<route.size();i++)if(route.get(i).checkpoint())result++;return result;}
    private void reset(Player player,Run run){
        run.next=run.saved+1;run.falls++;run.teleporting=true;player.setFallDistance(0);
        player.teleportAsync(at(run.wing,nodes(run.wing).get(run.saved))).whenComplete((ok,error)->{if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,()->run.teleporting=false);});
    }
    private void restart(Player player,Run run){run.saved=0;run.next=1;run.falls=0;run.started=System.nanoTime();resetWithoutFall(player,run);}
    private void resetWithoutFall(Player player,Run run){run.teleporting=true;player.setFallDistance(0);player.teleportAsync(at(run.wing,nodes(run.wing).get(run.saved))).whenComplete((ok,error)->{if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,()->run.teleporting=false);});}
    private void finish(Player player,Run run){
        runs.remove(player.getUniqueId());double seconds=(System.nanoTime()-run.started)/1e9;String key=run.wing.name()+"."+player.getUniqueId();double best=records.getDouble(key+".seconds",Double.MAX_VALUE);
        records.set(key+".name",player.getName());records.set(key+".completions",records.getInt(key+".completions")+1);
        if(seconds<best){records.set(key+".seconds",seconds);records.set(key+".falls",run.falls);}
        CompletableFuture<Void> persisted=saveRecords();Landing end=nodes(run.wing).getLast();
        player.showTitle(net.kyori.adventure.title.Title.title(Component.text("HOUSE OF WONDERS COMPLETE",NamedTextColor.GOLD),Component.text(String.format(Locale.ROOT,"%s — %.2fs",run.wing.name(),seconds))));
        player.getWorld().spawnParticle(Particle.FIREWORK,player.getLocation(),80,1.2,2,1.2,.05);
        player.teleportAsync(new Location(builder.world(),run.wing.x(end.u(),end.v())+.5,end.y()+4,run.wing.z(end.u(),end.v())+.5,run.wing.yaw()+180,0));
        player.sendMessage("Conclusão registrada; persistindo recorde... /wonder hub visita o átrio; /wonder leave restaura seu estado.");
        persisted.whenComplete((unused,error)->{if(!plugin.isEnabled())return;Bukkit.getScheduler().runTask(plugin,()->{if(!player.isOnline())return;if(error==null)player.sendMessage("Recorde V3 salvo com segurança.");else player.sendMessage("Falha ao salvar recorde; o erro foi preservado no console.");});});
    }
    private CompletableFuture<Void> saveRecords(){
        String yaml=records.saveToString();return CompletableFuture.runAsync(()->{
            try{Files.createDirectories(recordsFile.getParent());Path tmp=recordsFile.resolveSibling("wonder-v3-records.tmp");Files.writeString(tmp,yaml,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(tmp,recordsFile,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(tmp,recordsFile,StandardCopyOption.REPLACE_EXISTING);}}
            catch(Exception error){plugin.getLogger().log(Level.SEVERE,"Falha ao salvar recordes V3",error);throw new CompletionException(error);}
        },io);
    }
    private void leave(Player player,boolean teleportIfMissing){
        UUID id=player.getUniqueId();if(pending.contains(id))return;runs.remove(id);pending.add(id);
        if(snapshots.exists(id))snapshots.restore(player).whenComplete((ok,error)->{if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,()->{pending.remove(id);if(error!=null)player.sendMessage("Falha ao restaurar estado; snapshot V3 foi preservado.");});});
        else{pending.remove(id);Location origin=visitors.remove(id);if(origin!=null)player.teleportAsync(origin);else if(teleportIfMissing&&builder.ready())player.teleportAsync(hub());}
    }
    private void recover(Player player){if(snapshots.exists(player.getUniqueId()))leave(player,false);}
    private void verify(CommandSender sender){
        if(!builder.ready()){sender.sendMessage("Gere a V3 primeiro.");return;}World world=builder.world();Set<ChunkPos> keys=new HashSet<>();
        for(Wing wing:Wing.values())for(Stage stage:stages(wing))for(Landing node:stage.landings()){int x=wing.x(node.u(),node.v()),z=wing.z(node.u(),node.v());keys.add(new ChunkPos(x>>4,z>>4));}
        List<CompletableFuture<Chunk>> futures=keys.stream().map(pos->world.getChunkAtAsync(pos.x(),pos.z(),true)).toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenComplete((unused,error)->Bukkit.getScheduler().runTask(plugin,()->{
            if(error!=null){sender.sendMessage("Falha ao carregar chunks para verificação: "+error.getMessage());return;}List<Chunk> held=futures.stream().map(CompletableFuture::join).toList();held.forEach(chunk->chunk.addPluginChunkTicket(plugin));
            try{
                int checked=0;
                for(Wing wing:Wing.values())for(Stage stage:stages(wing))for(Landing node:stage.landings()){
                    for(int du=node.minOffset();du<=node.maxOffset();du++)for(int dv=node.minOffset();dv<=node.maxOffset();dv++){
                        int x=wing.x(node.u()+du,node.v()+dv),z=wing.z(node.u()+du,node.v()+dv);
                        if(world.getBlockAt(x,node.y(),z).getType().isAir())throw new IllegalStateException("footprint sem suporte "+wing+"/"+node.globalIndex()+"/"+du+","+dv);
                        for(int dy=1;dy<=3;dy++)if(!world.getBlockAt(x,node.y()+dy,z).getType().isAir())throw new IllegalStateException("headroom obstruído "+wing+"/"+node.globalIndex()+"/"+du+","+dv);
                    }checked++;
                }
                sender.sendMessage("WONDER_VERIFY_OK: "+checked+" pousos, 504 transições, 36 checkpoints, 12 fases.");
            }
            catch(Exception failure){sender.sendMessage("WONDER_VERIFY_FAILED: "+failure.getMessage());}finally{held.forEach(chunk->chunk.removePluginChunkTicket(plugin));}
        }));sender.sendMessage("Verificação V3 iniciada em "+keys.size()+" chunks...");
    }
    private void report(CommandSender sender){
        StringBuilder out=new StringBuilder("wing\tstage\ttransition\tcheckpoint\tobstacle\tx\ty\tz\n");
        for(Wing wing:Wing.values())for(Stage stage:stages(wing))for(Landing node:stage.landings())out.append(wing).append('\t').append(stage.number()).append('\t').append(node.transition()).append('\t').append(node.checkpoint()).append('\t').append(node.obstacle()).append('\t').append(wing.x(node.u(),node.v())).append('\t').append(node.y()+1).append('\t').append(wing.z(node.u(),node.v())).append('\n');
        Path target=plugin.getDataFolder().toPath().resolve("wonder-v3-coordinates.tsv");io.submit(()->{try{Files.createDirectories(target.getParent());Files.writeString(target,out);}catch(Exception error){plugin.getLogger().log(Level.SEVERE,"Falha ao exportar coordenadas V3",error);}});sender.sendMessage("Relatório solicitado: plugins/ArenaParkour/wonder-v3-coordinates.tsv (516 pontos).");
    }
    @EventHandler public void join(PlayerJoinEvent event){recover(event.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent event){UUID id=event.getPlayer().getUniqueId();runs.remove(id);pending.remove(id);Location origin=visitors.remove(id);if(origin!=null&&!snapshots.exists(id))event.getPlayer().teleport(origin);}
    @EventHandler public void damage(EntityDamageEvent event){if(event.getEntity() instanceof Player player&&inWorld(player)){event.setCancelled(true);if(event.getCause()==EntityDamageEvent.DamageCause.VOID){Run run=runs.get(player.getUniqueId());if(run!=null)reset(player,run);else if(!adapter.active(player.getUniqueId()))player.teleportAsync(hub());}}}
    @EventHandler public void breaking(BlockBreakEvent event){if(inWorld(event.getPlayer()))event.setCancelled(true);}
    @EventHandler public void placing(BlockPlaceEvent event){if(inWorld(event.getPlayer()))event.setCancelled(true);}
    @EventHandler public void hunger(FoodLevelChangeEvent event){if(event.getEntity() instanceof Player player&&inWorld(player))event.setFoodLevel(20);}
    @EventHandler public void interact(PlayerInteractEvent event){if(inWorld(event.getPlayer()))event.setCancelled(true);}
    @EventHandler public void drop(PlayerDropItemEvent event){if(inWorld(event.getPlayer()))event.setCancelled(true);}
    @EventHandler public void pickup(EntityPickupItemEvent event){if(event.getEntity() instanceof Player player&&inWorld(player))event.setCancelled(true);}
    @EventHandler public void teleport(PlayerTeleportEvent event){if(runs.containsKey(event.getPlayer().getUniqueId())&&(event.getCause()==PlayerTeleportEvent.TeleportCause.ENDER_PEARL||event.getCause()==PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT))event.setCancelled(true);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        try{
            String sub=args.length==0?"hub":args[0].toLowerCase(Locale.ROOT);
            if(sub.equals("generate")){if(sender.hasPermission("arena.parkour.admin"))builder.generate(sender);else sender.sendMessage("Sem permissão.");return true;}
            if(sub.equals("status")){sender.sendMessage("House of Wonders V3: "+builder.status()+", training="+runs.size());return true;}
            if(sub.equals("verify")){if(sender.hasPermission("arena.parkour.admin"))verify(sender);else sender.sendMessage("Sem permissão.");return true;}
            if(sub.equals("report")){if(sender.hasPermission("arena.parkour.admin"))report(sender);else sender.sendMessage("Sem permissão.");return true;}
            if(!(sender instanceof Player player)){sender.sendMessage("Use este comando no jogo.");return true;}
            if(adapter.active(player.getUniqueId())){player.sendMessage("Encerre sua partida oficial antes de usar a V3.");return true;}
            if(externalActive.test(player.getUniqueId())){player.sendMessage("Encerre a sessão Palace antes de usar a V3.");return true;}
            if(!builder.ready()){player.sendMessage("House of Wonders V3 ainda não está pronta.");return true;}
            switch(sub){
                case "hub" -> {if(runs.containsKey(player.getUniqueId()))player.sendMessage("Use /wonder leave para encerrar a tentativa, ou /wonder restart.");else{rememberVisitor(player);player.teleportAsync(hub());}}
                case "start" -> start(player,Wing.valueOf(args[1].toUpperCase(Locale.ROOT)));
                case "reset" -> {Run run=runs.get(player.getUniqueId());if(run!=null)reset(player,run);}
                case "restart" -> {Run run=runs.get(player.getUniqueId());if(run!=null)restart(player,run);}
                case "leave" -> leave(player,true);
                case "records" -> {for(Wing wing:Wing.values()){String key=wing.name()+"."+player.getUniqueId();player.sendMessage(wing.name()+": "+(records.contains(key)?String.format(Locale.ROOT,"%.2fs / %d quedas",records.getDouble(key+".seconds"),records.getInt(key+".falls")):"sem conclusão"));}}
                case "view" -> {if(runs.containsKey(player.getUniqueId())){player.sendMessage("Use /wonder leave antes de inspecionar.");break;}rememberVisitor(player);Wing wing=Wing.valueOf(args[1].toUpperCase(Locale.ROOT));int number=Integer.parseInt(args[2]);Stage stage=WonderRoute.stage(wing,number);Landing first=stage.landings().getFirst();player.teleportAsync(new Location(builder.world(),wing.x(first.u()+28,first.v())+.5,first.y()+10,wing.z(first.u()+28,first.v())+.5,wing.yaw()+90,18));}
                default -> sender.sendMessage("/wonder generate|status|verify|report|hub|start <easy|medium|hard|impossible>|reset|restart|leave|records|view <ala> <1..3>");
            }
        }catch(Exception error){sender.sendMessage("Uso inválido: "+error.getMessage());}
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(args.length==1)return List.of("generate","status","verify","report","hub","start","reset","restart","leave","records","view");
        if(args.length==2&&(args[0].equalsIgnoreCase("start")||args[0].equalsIgnoreCase("view")))return List.of("easy","medium","hard","impossible");
        if(args.length==3&&args[0].equalsIgnoreCase("view"))return List.of("1","2","3");return List.of();
    }
    @Override public void close(){
        ticker.cancel();
        for(Player player:Bukkit.getOnlinePlayers())if(snapshots.exists(player.getUniqueId()))try{snapshots.restoreNow(player);}catch(Exception error){plugin.getLogger().log(Level.SEVERE,"Falha ao restaurar snapshot V3 no disable; arquivo preservado para recovery",error);}
        for(var entry:visitors.entrySet()){Player player=Bukkit.getPlayer(entry.getKey());if(player!=null&&player.isOnline()&&!snapshots.exists(entry.getKey()))player.teleport(entry.getValue());}
        runs.clear();pending.clear();visitors.clear();snapshots.close();io.shutdown();
    }
}

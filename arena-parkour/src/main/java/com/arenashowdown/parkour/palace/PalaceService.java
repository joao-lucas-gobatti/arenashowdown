package com.arenashowdown.parkour.palace;

import com.arenashowdown.parkour.*;
import com.arenashowdown.parkour.arena.*;
import com.arenashowdown.parkour.player.PersistentSnapshotStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static com.arenashowdown.parkour.palace.PalaceRoute.*;

/** Training lifecycle is deliberately separate from official ArenaCore race results. */
public final class PalaceService implements Listener,CommandExecutor,TabCompleter,AutoCloseable {
    private final JavaPlugin plugin;private final ArenaStore arenas;private final ParkourAdapter adapter;
    private final PalaceBuilder builder;private final PersistentSnapshotStore snapshots;
    private final Map<UUID,Run> runs=new HashMap<>();private final Set<UUID> pending=new HashSet<>();
    private java.util.function.Predicate<UUID> externalActive=id->false;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final YamlConfiguration records;
    private TextDisplay leaderboard;
    private static final String TAG="grand_dice_palace_display";
    private static final String[][] STAGES={{"O SALAO DAS FICHAS","O CORREDOR DOS DADOS","O JARDIM DOURADO"},{"A BIBLIOTECA DAS CARTAS","A GALERIA SUSPENSA","O SALAO DAS ILUSÕES"},{"OS BASTIDORES","O MECANISMO DA ROLETA","O GRANDE PALCO"},{"O LABIRINTO DOS DADOS","O ABISMO DAS CARTAS","O TRONO DO DADO"}};
    private static final class Run {
        final Wing wing;final long started=System.nanoTime();int next=1,saved=0,falls;boolean teleporting;
        Run(Wing wing){this.wing=wing;}
    }
    public PalaceService(JavaPlugin plugin,ArenaStore arenas,ParkourAdapter adapter){
        this.plugin=plugin;this.arenas=arenas;this.adapter=adapter;
        snapshots=new PersistentSnapshotStore(plugin,plugin.getDataFolder().toPath().resolve("palace-snapshots"));
        records=YamlConfiguration.loadConfiguration(plugin.getDataFolder().toPath().resolve("palace-records.yml").toFile());
        builder=new PalaceBuilder(plugin,this::install);
        var cmd=Objects.requireNonNull(plugin.getCommand("palace"));cmd.setExecutor(this);cmd.setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this,plugin);
        if(builder.ready()){builder.world();Bukkit.getScheduler().runTask(plugin,this::install);}
        Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);
        Bukkit.getOnlinePlayers().forEach(this::recover);
    }
    public boolean active(UUID id){return runs.containsKey(id)||pending.contains(id)||snapshots.exists(id);}
    public void externalActive(java.util.function.Predicate<UUID> predicate){externalActive=Objects.requireNonNull(predicate);}
    private void install(){
        try{
            for(Wing wing:Wing.values())if(arenas.get(wing.id()).isEmpty())arenas.save(PalaceRoute.arena(wing));
            labels();
        }catch(Exception e){throw new IllegalStateException("Não foi possível cadastrar pistas",e);}
    }
    private void labels(){
        World world=builder.world();
        Set<Long> chunks=new HashSet<>();
        for(Wing w:Wing.values())for(int v:new int[]{27,45,73,101,114}){
            int x=w.x(0,v)>>4,z=w.z(0,v)>>4;long key=((long)x<<32)^(z&0xffffffffL);
            if(chunks.add(key))world.getChunkAt(x,z).addPluginChunkTicket(plugin);
        }
        world.getChunkAt(0,0).addPluginChunkTicket(plugin);
        world.getEntities().stream().filter(e->e.getScoreboardTags().contains(TAG)).forEach(Entity::remove);
        label(new Location(world,.5,77,.5),"THE GRAND DICE\nPARKOUR PALACE\n/palace start easy|medium|hard|impossible\n/palace leave",1.1f);
        leaderboard=world.spawn(new Location(world,.5,78,12.5),TextDisplay.class,d->{d.addScoreboardTag(TAG);d.setBillboard(Display.Billboard.CENTER);d.setLineWidth(500);d.setShadowed(true);});
        updateLeaderboard();
        for(Wing w:Wing.values()){
            label(new Location(world,w.x(0,27)+.5,80,w.z(0,27)+.5),w.name()+" — "+w.title+"\n/palace start "+w.name().toLowerCase(Locale.ROOT),.85f);
            for(int stage=0;stage<3;stage++)label(new Location(world,w.x(0,45+stage*28)+.5,87,w.z(0,45+stage*28)+.5),(stage+1)+" / 3 — "+STAGES[w.ordinal()][stage],.7f);
            label(new Location(world,w.x(0,114)+.5,80,w.z(0,114)+.5),"CHEGADA\n/palace hub",.7f);
        }
    }
    private void label(Location at,String text,float scale){at.getWorld().spawn(at,TextDisplay.class,d->{
        d.text(Component.text(text,NamedTextColor.GOLD));d.setBillboard(Display.Billboard.CENTER);d.setShadowed(true);d.setLineWidth(500);d.setViewRange(1.2f);
        var t=d.getTransformation();t.getScale().set(scale);d.setTransformation(t);d.addScoreboardTag(TAG);
    });}
    private boolean inWorld(Player p){return p.getWorld().getName().equals(WORLD);}
    private void updateLeaderboard(){
        if(leaderboard==null||!leaderboard.isValid())return;
        StringBuilder text=new StringBuilder("RECORDES DO PALACIO — TREINO\n");
        for(Wing w:Wing.values()){
            var section=records.getConfigurationSection(w.name());String winner="—";double best=Double.MAX_VALUE;
            if(section!=null)for(String id:section.getKeys(false)){double time=section.getDouble(id+".seconds",Double.MAX_VALUE);if(time<best){best=time;winner=section.getString(id+".name",id);}}
            text.append(w.name()).append(": ").append(winner);if(best<Double.MAX_VALUE)text.append(String.format(Locale.ROOT," — %.2fs",best));text.append('\n');
        }
        leaderboard.text(Component.text(text.toString(),NamedTextColor.GOLD));
    }
    private Location location(ArenaPoint p){return new Location(builder.world(),p.x(),p.y(),p.z(),p.yaw(),p.pitch());}
    private Location hub(){return new Location(builder.world(),.5,71,5.5,180,0);}
    private void start(Player p,Wing w){
        if(!builder.ready()){p.sendMessage("O palácio ainda não está pronto.");return;}
        if(adapter.active(p.getUniqueId())||pending.contains(p.getUniqueId())||externalActive.test(p.getUniqueId())){p.sendMessage("Aguarde o encerramento da partida/operação atual.");return;}
        if(runs.containsKey(p.getUniqueId())){p.sendMessage("Use /palace leave antes de iniciar outra tentativa.");return;}
        if(snapshots.exists(p.getUniqueId())){p.sendMessage("Use /palace leave para restaurar seu estado antes da próxima tentativa.");return;}
        if(!p.getActivePotionEffects().isEmpty()){p.sendMessage("Aguarde os efeitos de poção terminarem para um treino sem vantagens.");return;}
        pending.add(p.getUniqueId());
        snapshots.capture(p).whenComplete((unused,error)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                pending.remove(p.getUniqueId());if(error!=null){p.sendMessage("Falha ao salvar seu estado; tentativa não iniciada.");return;}
                if(!p.isOnline())return;
                Run run=new Run(w);runs.put(p.getUniqueId(),run);
                p.getInventory().clear();p.setGameMode(GameMode.ADVENTURE);p.setFlying(false);p.setAllowFlight(false);p.setFoodLevel(20);
                p.teleport(location(landings(w).getFirst().point(w)));
                p.sendMessage("Treino "+w.name()+": siga as plataformas claras; cobre indica checkpoint. /palace reset ou /palace leave.");
            });
        });
    }
    private void tick(){
        for(var entry:new ArrayList<>(runs.entrySet())){
            Player p=Bukkit.getPlayer(entry.getKey());if(p==null)continue;Run r=entry.getValue();
            if(adapter.active(p.getUniqueId())){runs.remove(p.getUniqueId());continue;}
            if(!inWorld(p)){leave(p,false);continue;}
            if(r.teleporting)continue;
            var nodes=landings(r.wing);var expected=nodes.get(r.next);Location l=p.getLocation();
            int relativeForward=r.wing.dx*l.getBlockX()+r.wing.dz*l.getBlockZ();
            int relativeSide=-r.wing.dz*l.getBlockX()+r.wing.dx*l.getBlockZ();
            if(p.isFlying()||p.isGliding()||p.getGameMode()!=GameMode.ADVENTURE){leave(p,true);p.sendMessage("Tentativa encerrada: voo/modo de jogo alterado.");continue;}
            if(l.getY()<nodes.get(r.saved).y()-1||Math.abs(relativeSide)>5||relativeForward>expected.v()+4){reset(p,r);continue;}
            if(p.isOnGround()&&expected.region(r.wing).contains(WORLD,l.getX(),l.getY(),l.getZ())){
                r.next++;
                if(expected.checkpoint()){
                    r.saved=expected.index();p.playSound(l,Sound.ENTITY_EXPERIENCE_ORB_PICKUP,.6f,1.4f);
                    p.sendMessage("Etapa "+expected.stage()+" / 3 concluída.");
                }
                if(r.next==nodes.size()){finish(p,r);continue;}
            }
            p.sendActionBar(Component.text(String.format(Locale.ROOT,"%s | etapa %d/3 | salto %d/%d | %.2fs | quedas %d",r.wing.name(),nodes.get(r.next).stage(),r.next,nodes.size()-1,(System.nanoTime()-r.started)/1e9,r.falls),NamedTextColor.GOLD));
        }
    }
    private void reset(Player p,Run r){
        r.next=r.saved+1;r.falls++;r.teleporting=true;p.setFallDistance(0);
        p.teleportAsync(location(landings(r.wing).get(r.saved).point(r.wing))).whenComplete((ok,error)->{
            if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,()->r.teleporting=false);
        });
    }
    private void finish(Player p,Run r){
        runs.remove(p.getUniqueId());double seconds=(System.nanoTime()-r.started)/1e9;
        String key=r.wing.name()+"."+p.getUniqueId();double best=records.getDouble(key+".seconds",Double.MAX_VALUE);
        records.set(key+".name",p.getName());records.set(key+".completions",records.getInt(key+".completions")+1);
        if(seconds<best){records.set(key+".seconds",seconds);records.set(key+".falls",r.falls);}
        updateLeaderboard();
        String yaml=records.saveToString();io.submit(()->{try{Path target=plugin.getDataFolder().toPath().resolve("palace-records.yml"),tmp=target.resolveSibling("palace-records.tmp");Files.writeString(tmp,yaml);Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Falha ao salvar recorde",e);}});
        p.showTitle(net.kyori.adventure.title.Title.title(Component.text("PALACE COMPLETE",NamedTextColor.GOLD),Component.text(String.format(Locale.ROOT,"%s — %.2fs",r.wing.name(),seconds))));
        p.getWorld().spawnParticle(Particle.FIREWORK,p.getLocation(),60,1,2,1,.05);
        p.teleportAsync(new Location(builder.world(),r.wing.x(0,120)+.5,77,r.wing.z(0,120)+.5,r.wing.yaw()+180,0));
        p.sendMessage("Conclusão de treino salva. /palace hub para o átrio; /palace leave restaura inventário e posição anteriores.");
    }
    private void leave(Player p,boolean teleport){
        if(pending.contains(p.getUniqueId()))return;
        runs.remove(p.getUniqueId());pending.add(p.getUniqueId());
        if(snapshots.exists(p.getUniqueId()))snapshots.restore(p).whenComplete((ok,error)->{if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,()->{pending.remove(p.getUniqueId());if(error!=null)p.sendMessage("Falha ao restaurar estado; snapshot preservado.");});});
        else{pending.remove(p.getUniqueId());if(teleport)p.teleport(hub());}
    }
    private void recover(Player p){if(snapshots.exists(p.getUniqueId()))leave(p,false);}
    @EventHandler public void join(PlayerJoinEvent e){recover(e.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent e){runs.remove(e.getPlayer().getUniqueId());pending.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void damage(EntityDamageEvent e){if(e.getEntity() instanceof Player p&&inWorld(p)){e.setCancelled(true);if(e.getCause()==EntityDamageEvent.DamageCause.VOID){Run r=runs.get(p.getUniqueId());if(r!=null)reset(p,r);else if(!adapter.active(p.getUniqueId()))p.teleport(hub());}}}
    @EventHandler public void breaking(BlockBreakEvent e){if(inWorld(e.getPlayer()))e.setCancelled(true);}
    @EventHandler public void placing(BlockPlaceEvent e){if(inWorld(e.getPlayer()))e.setCancelled(true);}
    @EventHandler public void hunger(FoodLevelChangeEvent e){if(e.getEntity() instanceof Player p&&inWorld(p))e.setFoodLevel(20);}
    @EventHandler public void interact(PlayerInteractEvent e){if(inWorld(e.getPlayer()))e.setCancelled(true);}
    @EventHandler public void drop(PlayerDropItemEvent e){if(inWorld(e.getPlayer()))e.setCancelled(true);}
    @EventHandler public void pickup(EntityPickupItemEvent e){if(e.getEntity() instanceof Player p&&inWorld(p))e.setCancelled(true);}
    @EventHandler public void pearl(PlayerTeleportEvent e){if(runs.containsKey(e.getPlayer().getUniqueId())&&(e.getCause()==PlayerTeleportEvent.TeleportCause.ENDER_PEARL||e.getCause()==PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT))e.setCancelled(true);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        try{
            String sub=args.length==0?"hub":args[0].toLowerCase(Locale.ROOT);
            if(sub.equals("generate")){if(sender.hasPermission("arena.parkour.admin"))builder.generate(sender);else sender.sendMessage("Sem permissão.");return true;}
            if(sub.equals("status")){sender.sendMessage("Palace ready="+builder.ready()+", training="+runs.size());return true;}
            if(sub.equals("verify")){
                if(!sender.hasPermission("arena.parkour.admin")||!builder.ready()){sender.sendMessage("Requer administrador e geração concluída.");return true;}
                World world=builder.world();int checked=0;
                for(Wing w:Wing.values())for(var n:landings(w)){
                    var at=n.point(w);int x=(int)Math.floor(at.x()),z=(int)Math.floor(at.z());
                    if(world.getBlockAt(x,n.y(),z).getType().isAir()||!world.getBlockAt(x,n.y()+1,z).getType().isAir()||!world.getBlockAt(x,n.y()+2,z).getType().isAir())throw new IllegalStateException("Pouso obstruído "+w+" / "+n.index());
                    checked++;
                }
                sender.sendMessage("PALACE_VERIFY_OK: "+checked+" pousos suportados e livres; 4 pistas; 12 finais de etapa.");return true;
            }
            if(!(sender instanceof Player p)){sender.sendMessage("Use este comando no jogo.");return true;}
            if(adapter.active(p.getUniqueId())){p.sendMessage("Encerre sua partida oficial antes de usar o palácio.");return true;}
            if(externalActive.test(p.getUniqueId())){p.sendMessage("Encerre a sessão House of Wonders antes de usar o Palace.");return true;}
            if(!builder.ready()){p.sendMessage("O palácio ainda está sendo preparado.");return true;}
            switch(sub){
                case "hub" -> {if(runs.containsKey(p.getUniqueId())||pending.contains(p.getUniqueId()))p.sendMessage("Use /palace leave para sair da tentativa.");else p.teleportAsync(hub());}
                case "start" -> start(p,Wing.valueOf(args[1].toUpperCase(Locale.ROOT)));
                case "reset" -> {Run r=runs.get(p.getUniqueId());if(r!=null)reset(p,r);}
                case "leave" -> leave(p,true);
                case "view" -> {if(runs.containsKey(p.getUniqueId())){p.sendMessage("Use /palace leave antes de inspecionar.");break;}Wing w=Wing.valueOf(args[1].toUpperCase(Locale.ROOT));int stage=Integer.parseInt(args[2]);if(stage<1||stage>3)throw new IllegalArgumentException("Etapa deve ser 1, 2 ou 3");p.teleportAsync(new Location(builder.world(),w.x(20,44+(stage-1)*28)+.5,79,w.z(20,44+(stage-1)*28)+.5,w.yaw()+90,15));}
                case "records" -> {for(Wing w:Wing.values()){String key=w.name()+"."+p.getUniqueId();p.sendMessage(w.name()+": "+(records.contains(key)?String.format(Locale.ROOT,"%.2fs",records.getDouble(key+".seconds")):"sem conclusão"));}}
                default -> sender.sendMessage("/palace generate|hub|start <easy|medium|hard|impossible>|reset|leave|records|view <ala> <1..3>");
            }
        }catch(Exception e){sender.sendMessage("Uso: /palace start <easy|medium|hard|impossible> ou /palace view <ala> <1..3>. "+e.getMessage());}
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] a){return a.length==1?List.of("generate","hub","start","reset","leave","records","view","status"):a.length==2?List.of("easy","medium","hard","impossible"):List.of("1","2","3");}
    @Override public void close(){
        for(Player player:Bukkit.getOnlinePlayers())if(snapshots.exists(player.getUniqueId()))try{snapshots.restoreNow(player);}catch(Exception error){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Falha ao restaurar snapshot Palace no disable; arquivo preservado para recovery",error);}
        runs.clear();pending.clear();snapshots.close();io.shutdown();
    }
}

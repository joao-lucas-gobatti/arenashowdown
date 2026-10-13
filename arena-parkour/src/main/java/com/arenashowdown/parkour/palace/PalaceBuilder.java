package com.arenashowdown.parkour.palace;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.nio.file.*;
import java.util.*;
import static com.arenashowdown.parkour.palace.PalaceRoute.WORLD;

public final class PalaceBuilder {
    private final JavaPlugin plugin; private boolean busy;
    private final Runnable complete;
    public PalaceBuilder(JavaPlugin plugin,Runnable complete){this.plugin=plugin;this.complete=complete;}
    public boolean ready(){return !busy&&Files.exists(marker());}
    private Path marker(){return plugin.getDataFolder().toPath().resolve("palace-v1.complete");}
    public World world(){
        World loaded=Bukkit.getWorld(WORLD);if(loaded!=null)return loaded;
        return new WorldCreator(WORLD).generateStructures(false).generator(new ChunkGenerator(){
            @Override public boolean shouldGenerateNoise(){return false;}
            @Override public boolean shouldGenerateSurface(){return false;}
            @Override public boolean shouldGenerateCaves(){return false;}
            @Override public boolean shouldGenerateDecorations(){return false;}
            @Override public boolean shouldGenerateMobs(){return false;}
            @Override public boolean shouldGenerateStructures(){return false;}
        }).createWorld();
    }
    public void generate(CommandSender sender){
        if(busy){sender.sendMessage("Geração do palácio já em andamento.");return;}
        if(Files.exists(marker())){sender.sendMessage("Palácio já gerado. Use /palace hub.");return;}
        Path worldPath=Bukkit.getWorldContainer().toPath().resolve(WORLD);
        Path pending=plugin.getDataFolder().toPath().resolve("palace-v1.pending");
        if(Files.exists(worldPath.resolve("level.dat"))&&!Files.exists(pending)){sender.sendMessage("Mundo existente sem marcador do gerador; geração recusada para preservar dados.");return;}
        try{Files.createDirectories(pending.getParent());Files.writeString(pending,"owned-world="+WORLD);}catch(Exception e){sender.sendMessage(e.getMessage());return;}
        busy=true;World world=world();if(world==null){busy=false;return;}
        sender.sendMessage("Preparando The Grand Dice Palace; 4 alas, 12 ambientes.");
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            try{
                var blocks=PalaceArchitecture.build().ordered();
                Bukkit.getScheduler().runTask(plugin,()->new BukkitRunnable(){
                    int cursor,ticks;boolean loading;Chunk held;
                    final Map<String,org.bukkit.block.data.BlockData> states=new HashMap<>();
                    @Override public void run(){
                        if(loading)return;
                        try{
                            long deadline=System.nanoTime()+3_000_000L;int count=0;
                            while(cursor<blocks.size()&&count++<2500&&System.nanoTime()<deadline){
                                var block=blocks.get(cursor);var p=block.pos();int cx=p.x()>>4,cz=p.z()>>4;
                                if(held==null||held.getX()!=cx||held.getZ()!=cz){
                                    if(held!=null)held.removePluginChunkTicket(plugin);held=null;loading=true;
                                    world.getChunkAtAsync(cx,cz,true).whenComplete((chunk,error)->{
                                        if(!plugin.isEnabled())return;
                                        Bukkit.getScheduler().runTask(plugin,()->{loading=false;if(error!=null){cancel();fail(error);return;}held=chunk;held.addPluginChunkTicket(plugin);});
                                    });return;
                                }
                                var target=world.getBlockAt(p.x(),p.y(),p.z());
                                if(block.data()==null)target.setType(block.material(),false);
                                else target.setBlockData(states.computeIfAbsent(block.data(),Bukkit::createBlockData),false);
                                cursor++;
                            }
                            if(++ticks%100==0)sender.sendMessage("Palácio: "+cursor*100/blocks.size()+"%");
                            if(cursor==blocks.size()){
                                cancel();if(held!=null)held.removePluginChunkTicket(plugin);
                                world.setSpawnLocation(0,71,0);world.setTime(18000);
                                world.setGameRule(GameRule.DO_MOB_SPAWNING,false);world.setGameRule(GameRule.DO_FIRE_TICK,false);
                                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);
                                world.setStorm(false);world.setDifficulty(Difficulty.PEACEFUL);
                                complete.run();world.save();Files.writeString(marker(),"world="+WORLD+"\nblocks="+blocks.size());
                                busy=false;sender.sendMessage("PALACE_READY: "+blocks.size()+" blocos aplicados; quatro pistas cadastradas.");
                            }
                        }catch(Exception e){cancel();if(held!=null)held.removePluginChunkTicket(plugin);fail(e);}
                    }
                }.runTaskTimer(plugin,1,1));
            }catch(Exception e){Bukkit.getScheduler().runTask(plugin,()->fail(e));}
        });
    }
    private void fail(Throwable e){busy=false;plugin.getLogger().log(java.util.logging.Level.SEVERE,"Falha no palácio; lobby preservado. /palace generate retoma por reaplicação.",e);}
}

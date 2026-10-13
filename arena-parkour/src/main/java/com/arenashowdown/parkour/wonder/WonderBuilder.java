package com.arenashowdown.parkour.wonder;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import static com.arenashowdown.parkour.wonder.WonderRoute.*;

/** Crash-resumable V3 world applicator. Planning is async; all Bukkit block writes stay on the main thread. */
public final class WonderBuilder {
    private static final String COMPLETE="wonder-v3.complete";
    private static final String PENDING="wonder-v3.pending";
    public static final String BLUEPRINT=SCHEMA+"-architecture-r2";
    private final JavaPlugin plugin;private final Runnable complete;private boolean busy;
    public WonderBuilder(JavaPlugin plugin,Runnable complete){this.plugin=plugin;this.complete=complete;}
    public boolean busy(){return busy;}
    public Path marker(){return plugin.getDataFolder().toPath().resolve(COMPLETE);}
    private Path pending(){return plugin.getDataFolder().toPath().resolve(PENDING);}
    private Path worldPath(){return Bukkit.getWorldContainer().toPath().resolve(WORLD);}
    public boolean ready(){
        if(busy||!Files.isRegularFile(marker())||!Files.isRegularFile(worldPath().resolve("level.dat")))return false;
        try{Properties value=readManifest(marker());UUID diskUuid=readWorldUuid();return "complete".equals(value.getProperty("state"))&&SCHEMA.equals(value.getProperty("schema"))&&BLUEPRINT.equals(value.getProperty("blueprint"))&&WORLD.equals(value.getProperty("world"))&&diskUuid.toString().equals(value.getProperty("world-uuid"));}
        catch(Exception ignored){return false;}
    }
    public String status(){return "ready="+ready()+", busy="+busy+", schema="+SCHEMA+", blueprint="+BLUEPRINT+", world="+WORLD;}
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
        if(busy){sender.sendMessage("House of Wonders V3: geração já em andamento.");return;}
        if(ready()){sender.sendMessage("House of Wonders V3 já está pronta. Use /wonder hub.");return;}
        if(Files.exists(marker())&&!validPending()){sender.sendMessage("Manifesto V3 incompatível sem retomada autorizada; geração recusada para preservar dados.");return;}
        Path level=worldPath().resolve("level.dat");if(Files.exists(level)&&!validPending()){sender.sendMessage("Mundo existente sem manifesto de propriedade V3; geração recusada para preservar dados.");return;}
        try{
            Files.createDirectories(plugin.getDataFolder().toPath());if(!Files.isRegularFile(level))writeManifest(pending(),baseManifest("pending",null));
            World world=world();if(world==null){sender.sendMessage("Não foi possível criar o mundo V3.");return;}
            writeManifest(pending(),baseManifest("pending",world.getUID()));busy=true;
            sender.sendMessage("House of Wonders V3: compondo 12 fases e 504 transições...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                try{List<WonderBlocks.Block> plan=WonderArchitecture.build().ordered();Bukkit.getScheduler().runTask(plugin,()->apply(world,plan,sender));}
                catch(Throwable error){Bukkit.getScheduler().runTask(plugin,()->fail(sender,error));}
            });
        }catch(Exception error){busy=false;sender.sendMessage("Não foi possível preparar o manifesto V3: "+error.getMessage());}
    }
    private boolean validPending(){
        try{
            if(!Files.isRegularFile(pending()))return false;Properties value=readManifest(pending());
            if(!"pending".equals(value.getProperty("state"))||!SCHEMA.equals(value.getProperty("schema"))||!BLUEPRINT.equals(value.getProperty("blueprint"))||!WORLD.equals(value.getProperty("world")))return false;
            if(Files.isRegularFile(worldPath().resolve("level.dat")))return readWorldUuid().toString().equals(value.getProperty("world-uuid"));
            return value.getProperty("world-uuid","").isBlank();
        }catch(Exception ignored){return false;}
    }
    private void apply(World world,List<WonderBlocks.Block> plan,CommandSender sender){
        new BukkitRunnable(){
            int cursor,lastPercent=-1;boolean loading;Chunk held;final Map<String,org.bukkit.block.data.BlockData> states=new HashMap<>();
            @Override public void run(){
                if(loading)return;
                try{
                    long deadline=System.nanoTime()+3_000_000L;int operations=0;
                    while(cursor<plan.size()&&operations++<2500&&System.nanoTime()<deadline){
                        WonderBlocks.Block block=plan.get(cursor);WonderBlocks.Pos pos=block.pos();int cx=pos.x()>>4,cz=pos.z()>>4;
                        if(held==null||held.getX()!=cx||held.getZ()!=cz){
                            release();loading=true;world.getChunkAtAsync(cx,cz,true).whenComplete((chunk,error)->{
                                if(!plugin.isEnabled())return;Bukkit.getScheduler().runTask(plugin,()->{loading=false;if(error!=null){cancel();fail(sender,error);return;}held=chunk;held.addPluginChunkTicket(plugin);});
                            });return;
                        }
                        var target=world.getBlockAt(pos.x(),pos.y(),pos.z());if(block.data()==null)target.setType(block.material(),false);else target.setBlockData(states.computeIfAbsent(block.data(),Bukkit::createBlockData),false);cursor++;
                    }
                    int percent=cursor*100/Math.max(1,plan.size());if(percent/10>lastPercent/10){lastPercent=percent;sender.sendMessage("House of Wonders V3: "+percent+"% ("+cursor+"/"+plan.size()+")");}
                    if(cursor==plan.size())finish(world,plan.size(),sender);
                }catch(Throwable error){cancel();release();fail(sender,error);}
            }
            private void release(){if(held!=null){held.removePluginChunkTicket(plugin);held=null;}}
            private void finish(World world,int blockCount,CommandSender sender)throws Exception{
                cancel();release();world.setSpawnLocation(0,FLOOR+1,8);world.setTime(6000);
                world.setGameRule(GameRule.DO_MOB_SPAWNING,false);world.setGameRule(GameRule.DO_FIRE_TICK,false);world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);world.setGameRule(GameRule.KEEP_INVENTORY,true);
                world.setStorm(false);world.setDifficulty(Difficulty.PEACEFUL);world.save();
                Properties committed=baseManifest("complete",world.getUID());committed.setProperty("blocks",String.valueOf(blockCount));committed.setProperty("completed",Instant.now().toString());writeManifest(marker(),committed);
                complete.run();Files.deleteIfExists(pending());busy=false;sender.sendMessage("WONDER_V3_READY: "+blockCount+" blocos; 12 fases; 504 transições; 4 arenas cadastradas.");
            }
        }.runTaskTimer(plugin,1,1);
    }
    private Properties baseManifest(String state,UUID worldUuid){Properties value=new Properties();value.setProperty("state",state);value.setProperty("schema",SCHEMA);value.setProperty("blueprint",BLUEPRINT);value.setProperty("world",WORLD);value.setProperty("updated",Instant.now().toString());if(worldUuid!=null)value.setProperty("world-uuid",worldUuid.toString());return value;}
    private Properties readManifest(Path path)throws IOException{Properties value=new Properties();try(Reader reader=Files.newBufferedReader(path)){value.load(reader);}return value;}
    private void writeManifest(Path target,Properties value)throws IOException{
        Files.createDirectories(target.getParent());Path temporary=target.resolveSibling(target.getFileName()+".tmp");
        try(Writer writer=Files.newBufferedWriter(temporary,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING)){value.store(writer,"Arena Showdown owned generated world");}
        try(FileChannel channel=FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}
        try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
    }
    private UUID readWorldUuid()throws IOException{try(DataInputStream input=new DataInputStream(Files.newInputStream(worldPath().resolve("uid.dat")))){return new UUID(input.readLong(),input.readLong());}}
    private void fail(CommandSender sender,Throwable error){busy=false;plugin.getLogger().log(Level.SEVERE,"Falha na geração V3; pending preservado para retomada idempotente.",error);sender.sendMessage("Falha na geração V3; Palace V2 permanece preservado. Consulte o console.");}
}

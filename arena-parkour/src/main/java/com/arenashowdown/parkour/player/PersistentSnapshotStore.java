package com.arenashowdown.parkour.player;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Atomic, validated player-state snapshots. Bukkit state is captured/applied only on the main thread. */
public final class PersistentSnapshotStore implements AutoCloseable {
    private static final String SCHEMA="parkour-snapshot-v2";
    private final Plugin plugin;private final Path directory;
    private final ExecutorService io=Executors.newSingleThreadExecutor(Thread.ofPlatform().name("parkour-snapshot-",0).factory());
    public PersistentSnapshotStore(Plugin plugin,Path directory){this.plugin=plugin;this.directory=directory;}

    public CompletableFuture<Void> capture(Player player){
        ensureMain();YamlConfiguration yaml=new YamlConfiguration();Location location=player.getLocation();
        yaml.set("schema",SCHEMA);yaml.set("uuid",player.getUniqueId().toString());yaml.set("world",player.getWorld().getName());
        yaml.set("x",location.getX());yaml.set("y",location.getY());yaml.set("z",location.getZ());yaml.set("yaw",location.getYaw());yaml.set("pitch",location.getPitch());
        yaml.set("game-mode",player.getGameMode().name());yaml.set("health",player.getHealth());yaml.set("food",player.getFoodLevel());
        yaml.set("allow-flight",player.getAllowFlight());yaml.set("flying",player.isFlying());
        ItemStack[] inventory=player.getInventory().getContents().clone(),armor=player.getInventory().getArmorContents().clone();
        yaml.set("inventory-size",inventory.length);yaml.set("armor-size",armor.length);yaml.set("inventory",Arrays.asList(inventory));yaml.set("armor",Arrays.asList(armor));
        String data=yaml.saveToString();UUID id=player.getUniqueId();
        return CompletableFuture.runAsync(()->writeAtomicIfAbsent(id,data),io);
    }
    public CompletableFuture<Boolean> restore(Player player){
        ensureMain();UUID id=player.getUniqueId();
        return CompletableFuture.supplyAsync(()->readValidated(id),io).thenCompose(yaml->{
            if(yaml==null)return CompletableFuture.completedFuture(false);CompletableFuture<Boolean> done=new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin,()->{
                if(!player.isOnline()){done.complete(false);return;}
                try{apply(player,yaml);CompletableFuture.runAsync(()->delete(id),io).whenComplete((unused,error)->{if(error==null)done.complete(true);else done.completeExceptionally(error);});}
                catch(Exception error){done.completeExceptionally(error);}
            });return done;
        });
    }
    /** Used during plugin disable while the main thread and player objects are still available. */
    public boolean restoreNow(Player player){
        ensureMain();if(!player.isOnline())return false;UUID id=player.getUniqueId();YamlConfiguration yaml=readValidated(id);if(yaml==null)return false;
        apply(player,yaml);delete(id);return true;
    }
    public boolean exists(UUID id){return Files.isRegularFile(file(id));}

    void writeAtomicIfAbsent(UUID id,String data){
        Path target=file(id);if(Files.exists(target))throw new CompletionException(new FileAlreadyExistsException("Snapshot pendente requer recovery antes de nova captura: "+id));Path temporary=directory.resolve(id+".tmp-"+UUID.randomUUID());
        try{
            Files.createDirectories(directory);Files.writeString(temporary,data,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
            try(FileChannel channel=FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}
            try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,target);}
        }catch(FileAlreadyExistsException error){
            try{Files.deleteIfExists(temporary);}catch(IOException suppressed){error.addSuppressed(suppressed);}throw new CompletionException(error);
        }catch(IOException error){
            try{Files.deleteIfExists(temporary);}catch(IOException suppressed){error.addSuppressed(suppressed);}throw new CompletionException(error);
        }
    }
    YamlConfiguration readValidated(UUID id){
        Path source=file(id);if(!Files.isRegularFile(source))return null;
        try{if(Files.size(source)==0)throw new IOException("Snapshot vazio para "+id);}catch(IOException error){throw new CompletionException(error);}
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(source.toFile());String stored=yaml.getString("uuid"),world=yaml.getString("world"),mode=yaml.getString("game-mode"),schema=yaml.getString("schema");
        boolean numeric=List.of("x","y","z","yaw","pitch","health","food").stream().allMatch(path->yaml.get(path) instanceof Number);
        boolean complete=numeric&&yaml.get("allow-flight") instanceof Boolean&&yaml.get("flying") instanceof Boolean&&yaml.isList("inventory")&&yaml.isList("armor");
        if(!id.toString().equals(stored)||world==null||world.isBlank()||mode==null||!complete)throw new CompletionException(new IOException("Snapshot incompleto ou inválido para "+id));
        try{GameMode.valueOf(mode);}catch(IllegalArgumentException error){throw new CompletionException(new IOException("Game mode inválido no snapshot de "+id,error));}
        for(String path:List.of("x","y","z","yaw","pitch","health","food"))if(!Double.isFinite(((Number)yaml.get(path)).doubleValue()))throw new CompletionException(new IOException("Valor não finito em "+path+" para "+id));
        double health=yaml.getDouble("health");int food=yaml.getInt("food");List<?> inventory=yaml.getList("inventory"),armor=yaml.getList("armor");
        int expectedInventory=schema==null?41:yaml.getInt("inventory-size",-1),expectedArmor=schema==null?4:yaml.getInt("armor-size",-1);
        if(schema!=null&&!SCHEMA.equals(schema))throw new CompletionException(new IOException("Schema de snapshot incompatível: "+schema));
        if(health<=0||health>2048||food<0||food>20||expectedInventory<1||expectedInventory>100||expectedArmor!=4||inventory==null||inventory.size()!=expectedInventory||armor==null||armor.size()!=expectedArmor||!validItems(inventory)||!validItems(armor))
            throw new CompletionException(new IOException("Estado de jogador truncado ou inválido no snapshot de "+id));
        return yaml;
    }
    private boolean validItems(List<?> values){return values.stream().allMatch(value->value==null||value instanceof ItemStack);}
    private void apply(Player player,YamlConfiguration yaml){
        World world=Bukkit.getWorld(Objects.requireNonNull(yaml.getString("world")));if(world==null)throw new IllegalStateException("Mundo original indisponível: "+yaml.getString("world"));
        player.teleport(new Location(world,yaml.getDouble("x"),yaml.getDouble("y"),yaml.getDouble("z"),(float)yaml.getDouble("yaw"),(float)yaml.getDouble("pitch")));
        player.setGameMode(GameMode.valueOf(yaml.getString("game-mode","SURVIVAL")));player.setHealth(Math.min(yaml.getDouble("health",20),player.getMaxHealth()));player.setFoodLevel(yaml.getInt("food",20));
        player.setAllowFlight(yaml.getBoolean("allow-flight",false));player.setFlying(yaml.getBoolean("flying",false)&&player.getAllowFlight());
        player.getInventory().setContents(items(yaml.getList("inventory"),player.getInventory().getSize()));player.getInventory().setArmorContents(items(yaml.getList("armor"),4));player.setCollidable(true);
    }
    private ItemStack[] items(List<?> list,int size){ItemStack[] result=new ItemStack[size];if(list!=null)for(int i=0;i<Math.min(size,list.size());i++)if(list.get(i) instanceof ItemStack item)result[i]=item;return result;}
    private void delete(UUID id){try{Files.deleteIfExists(file(id));}catch(IOException error){throw new CompletionException(error);}}
    private Path file(UUID id){return directory.resolve(id+".yml");}
    private void ensureMain(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Snapshot deve capturar/restaurar jogadores na thread principal");}
    @Override public void close(){io.shutdown();}
}

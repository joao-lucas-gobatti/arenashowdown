package com.arenashowdown.parkour.player;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class PersistentSnapshotStore implements AutoCloseable {
    private final Plugin plugin; private final Path directory; private final ExecutorService io=Executors.newSingleThreadExecutor(Thread.ofPlatform().name("parkour-snapshot-",0).factory());
    public PersistentSnapshotStore(Plugin plugin,Path directory){this.plugin=plugin;this.directory=directory;}
    public CompletableFuture<Void> capture(Player p){ensureMain();YamlConfiguration y=new YamlConfiguration();y.set("uuid",p.getUniqueId().toString());y.set("world",p.getWorld().getName());y.set("x",p.getLocation().getX());y.set("y",p.getLocation().getY());y.set("z",p.getLocation().getZ());y.set("yaw",p.getLocation().getYaw());y.set("pitch",p.getLocation().getPitch());y.set("game-mode",p.getGameMode().name());y.set("health",p.getHealth());y.set("food",p.getFoodLevel());y.set("allow-flight",p.getAllowFlight());y.set("flying",p.isFlying());y.set("inventory",Arrays.asList(p.getInventory().getContents().clone()));y.set("armor",Arrays.asList(p.getInventory().getArmorContents().clone()));String data=y.saveToString();return CompletableFuture.runAsync(()->{try{Files.createDirectories(directory);Files.writeString(file(p.getUniqueId()),data,StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}catch(IOException e){throw new CompletionException(e);}},io);}
    public CompletableFuture<Boolean> restore(Player p){ensureMain();UUID id=p.getUniqueId();return CompletableFuture.supplyAsync(()->{Path f=file(id);if(!Files.exists(f))return null;YamlConfiguration y=YamlConfiguration.loadConfiguration(f.toFile());return y;},io).thenCompose(y->{if(y==null)return CompletableFuture.completedFuture(false);CompletableFuture<Boolean> done=new CompletableFuture<>();Bukkit.getScheduler().runTask(plugin,()->{try{World w=Bukkit.getWorld(y.getString("world",""));if(w!=null)p.teleport(new Location(w,y.getDouble("x"),y.getDouble("y"),y.getDouble("z"),(float)y.getDouble("yaw"),(float)y.getDouble("pitch")));p.setGameMode(GameMode.valueOf(y.getString("game-mode","SURVIVAL")));p.setHealth(Math.min(y.getDouble("health",20),p.getMaxHealth()));p.setFoodLevel(y.getInt("food",20));p.setAllowFlight(y.getBoolean("allow-flight",false));p.setFlying(y.getBoolean("flying",false)&&p.getAllowFlight());p.getInventory().setContents(items(y.getList("inventory"),p.getInventory().getSize()));p.getInventory().setArmorContents(items(y.getList("armor"),4));p.setCollidable(true);CompletableFuture.runAsync(()->{try{Files.deleteIfExists(file(id));done.complete(true);}catch(IOException e){done.completeExceptionally(e);}},io);}catch(Exception e){done.completeExceptionally(e);}});return done;});}
    public boolean exists(UUID id){return Files.exists(file(id));}
    private ItemStack[] items(List<?> list,int size){ItemStack[] result=new ItemStack[size];if(list!=null)for(int i=0;i<Math.min(size,list.size());i++)if(list.get(i) instanceof ItemStack item)result[i]=item;return result;}
    private Path file(UUID id){return directory.resolve(id+".yml");} private void ensureMain(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Snapshot deve capturar/restaurar jogadores na thread principal");}
    @Override public void close(){io.shutdown();}
}

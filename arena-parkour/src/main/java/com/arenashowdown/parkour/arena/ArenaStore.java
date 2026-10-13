package com.arenashowdown.parkour.arena;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class ArenaStore {
    private final Path directory; private final Map<String,ParkourArena> arenas=new LinkedHashMap<>();
    public ArenaStore(Path directory){this.directory=directory;}
    public void loadAll()throws IOException{Files.createDirectories(directory);arenas.clear();try(var files=Files.list(directory)){for(Path file:files.filter(p->p.toString().endsWith(".yml")).toList()){ParkourArena arena=read(file);arenas.put(arena.name().toLowerCase(Locale.ROOT),arena);}}}
    public Optional<ParkourArena> get(String name){return Optional.ofNullable(arenas.get(name.toLowerCase(Locale.ROOT)));}
    public Collection<ParkourArena> all(){return List.copyOf(arenas.values());}
    public void put(ParkourArena arena){arenas.put(arena.name().toLowerCase(Locale.ROOT),arena);}
    public void save(ParkourArena arena)throws IOException{Files.createDirectories(directory);YamlConfiguration y=new YamlConfiguration();y.set("name",arena.name());y.set("world",arena.world());writePoint(y,"start",arena.start());writePoints(y,"spawns",arena.spawns());for(int i=0;i<arena.checkpoints().size();i++)writeRegion(y,"checkpoints."+i,arena.checkpoints().get(i));writeRegion(y,"finish",arena.finish());writePoint(y,"lobby",arena.lobby());y.set("time-limit-seconds",arena.timeLimitSeconds());y.set("fall-height",arena.fallHeight());y.set("rules",arena.rules());try(Writer out=Files.newBufferedWriter(directory.resolve(arena.name().toLowerCase(Locale.ROOT)+".yml"))){out.write(y.saveToString());}put(arena);}
    private ParkourArena read(Path file)throws IOException{YamlConfiguration y=YamlConfiguration.loadConfiguration(file.toFile());String name=y.getString("name",file.getFileName().toString().replace(".yml",""));String world=y.getString("world");List<ArenaPoint>spawns=new ArrayList<>();ConfigurationSection ss=y.getConfigurationSection("spawns");if(ss!=null)ss.getKeys(false).stream().sorted(Comparator.comparingInt(Integer::parseInt)).forEach(k->spawns.add(readPoint(ss.getConfigurationSection(k))));List<BlockRegion>checks=new ArrayList<>();ConfigurationSection cs=y.getConfigurationSection("checkpoints");if(cs!=null)cs.getKeys(false).stream().sorted(Comparator.comparingInt(Integer::parseInt)).forEach(k->checks.add(readRegion(cs.getConfigurationSection(k))));ConfigurationSection rules=y.getConfigurationSection("rules");return new ParkourArena(name,world,readPoint(y.getConfigurationSection("start")),spawns,checks,readRegion(y.getConfigurationSection("finish")),readPoint(y.getConfigurationSection("lobby")),y.getLong("time-limit-seconds",300),y.getDouble("fall-height",50),rules==null?Map.of():rules.getValues(false));}
    private static void writePoint(YamlConfiguration y,String path,ArenaPoint p){if(p==null)return;y.set(path+".world",p.world());y.set(path+".x",p.x());y.set(path+".y",p.y());y.set(path+".z",p.z());y.set(path+".yaw",p.yaw());y.set(path+".pitch",p.pitch());}
    private static void writePoints(YamlConfiguration y,String path,List<ArenaPoint>points){for(int i=0;i<points.size();i++)writePoint(y,path+"."+i,points.get(i));}
    private static ArenaPoint readPoint(ConfigurationSection s){return s==null?null:new ArenaPoint(s.getString("world"),s.getDouble("x"),s.getDouble("y"),s.getDouble("z"),(float)s.getDouble("yaw"),(float)s.getDouble("pitch"));}
    private static void writeRegion(YamlConfiguration y,String path,BlockRegion r){if(r==null)return;y.set(path+".world",r.world());y.set(path+".min-x",r.minX());y.set(path+".min-y",r.minY());y.set(path+".min-z",r.minZ());y.set(path+".max-x",r.maxX());y.set(path+".max-y",r.maxY());y.set(path+".max-z",r.maxZ());}
    private static BlockRegion readRegion(ConfigurationSection s){return s==null?null:new BlockRegion(s.getString("world"),s.getInt("min-x"),s.getInt("min-y"),s.getInt("min-z"),s.getInt("max-x"),s.getInt("max-y"),s.getInt("max-z"));}
}

package com.arenashowdown.tntrun.arena;

import com.arenashowdown.tntrun.persistence.AtomicFiles;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Reload is transactional: invalid YAML never replaces the currently validated set. */
public final class ArenaStore {
    private final Path directory;
    private volatile Map<String, TntArena> arenas = Map.of();
    public ArenaStore(Path directory) { this.directory = directory; }
    public Path directory() { return directory; }
    public Optional<TntArena> get(String id) { return Optional.ofNullable(arenas.get(id)); }
    public Collection<TntArena> all() { return List.copyOf(arenas.values()); }
    public void reload() throws Exception {
        Files.createDirectories(directory);
        Map<String, TntArena> loaded = new TreeMap<>();
        try (var files = Files.list(directory)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".yml")).sorted().toList()) {
                TntArena arena = parse(read(path));
                if (!path.getFileName().toString().equals(arena.id() + ".yml")) throw new IOException("Arena id must match filename: " + path);
                if (loaded.putIfAbsent(arena.id(), arena) != null) throw new IOException("Duplicate arena " + arena.id());
            }
        }
        validateOverlap(loaded.values());
        arenas = Map.copyOf(loaded);
    }
    public void save(YamlConfiguration yaml) throws Exception {
        TntArena arena = parse(yaml);
        Map<String, TntArena> updated = new TreeMap<>(arenas); updated.put(arena.id(), arena);
        validateOverlap(updated.values());
        AtomicFiles.write(directory.resolve(arena.id() + ".yml"), yaml.saveToString(), true);
        arenas = Map.copyOf(updated);
    }
    private void validateOverlap(Collection<TntArena> values) {
        List<TntArena> list = List.copyOf(values);
        for (int i = 0; i < list.size(); i++) for (int j = i + 1; j < list.size(); j++)
            if (list.get(i).world().equals(list.get(j).world()) && list.get(i).bounds().overlaps(list.get(j).bounds()))
                throw new IllegalArgumentException("Overlapping arenas: " + list.get(i).id() + "/" + list.get(j).id());
    }
    public static YamlConfiguration read(Path path) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration(); yaml.loadFromString(Files.readString(path)); return yaml;
    }
    public static TntArena parse(YamlConfiguration y) {
        ConfigurationSection b = required(y, "bounds");
        List<TntArena.Layer> layers = new ArrayList<>();
        ConfigurationSection ls = required(y, "layers");
        for (String key : orderedKeys(ls)) {
            ConfigurationSection l = required(ls, key);
            layers.add(new TntArena.Layer(integer(l,"min-x"), integer(l,"max-x"), integer(l,"min-z"), integer(l,"max-z"), integer(l,"top-y"), integer(l,"depth")));
        }
        List<TntArena.Point> spawns = new ArrayList<>(); ConfigurationSection ss = required(y, "spawns");
        for (String key : orderedKeys(ss)) spawns.add(point(required(ss, key)));
        Set<String> materials = new HashSet<>(y.getStringList("materials"));
        for (String name : materials) {
            Material material = Material.matchMaterial(name);
            boolean floor = Set.of("SAND","RED_SAND","GRAVEL","TNT","STONE","SMOOTH_STONE","SANDSTONE","RED_SANDSTONE","TERRACOTTA").contains(name)
                || name.endsWith("_WOOL") || name.endsWith("_CONCRETE") || name.endsWith("_TERRACOTTA");
            if (material == null || !floor || !material.name().equals(name))
                throw new IllegalArgumentException("Invalid floor material " + name);
        }
        return new TntArena(y.getString("id"), y.getString("world"),
            new TntArena.Bounds(integer(b,"min-x"),integer(b,"min-y"),integer(b,"min-z"),integer(b,"max-x"),integer(b,"max-y"),integer(b,"max-z")),
            layers, spawns, point(required(y,"spectator")), number(y,"elimination-y"), integer(y,"timeout-seconds"), integer(y,"removal-delay-ticks"), materials);
    }
    private static List<String> orderedKeys(ConfigurationSection s) {
        return s.getKeys(false).stream().sorted(Comparator.comparingInt(Integer::parseInt)).toList();
    }
    private static ConfigurationSection required(ConfigurationSection s, String key) {
        ConfigurationSection result = s.getConfigurationSection(key);
        if (result == null) throw new IllegalArgumentException("Missing section " + key); return result;
    }
    private static int integer(ConfigurationSection s, String key) {
        Object n = s.get(key);
        if (!(n instanceof Number v) || !Double.isFinite(v.doubleValue()) || v.doubleValue() != v.intValue())
            throw new IllegalArgumentException("Invalid integer " + key); return v.intValue();
    }
    private static double number(ConfigurationSection s, String key) {
        Object n = s.get(key); if (!(n instanceof Number v) || !Double.isFinite(v.doubleValue())) throw new IllegalArgumentException("Invalid number " + key); return v.doubleValue();
    }
    private static TntArena.Point point(ConfigurationSection s) {
        return new TntArena.Point(number(s,"x"),number(s,"y"),number(s,"z"),(float)number(s,"yaw"),(float)number(s,"pitch"));
    }
}

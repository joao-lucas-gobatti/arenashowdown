package com.arenashowdown.tntrun.persistence;

import com.arenashowdown.tntrun.arena.*;
import org.bukkit.*;
import org.bukkit.block.TileState;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;

/** Complete original floors persisted BEFORE any mutation; recovery is idempotent. */
public final class BlockJournal {
    public record Cell(int x, int y, int z) {}
    private final Path file;
    public BlockJournal(Path directory) { file = directory.resolve("blocks.yml"); }
    public boolean pending() { return Files.exists(file); }
    public String matchId() throws Exception { return pending() ? validated().getString("match") : null; }
    public void capture(TntArena arena, World world, String match) throws Exception {
        if (pending()) throw new IllegalStateException("Unrestored block journal");
        YamlConfiguration y = new YamlConfiguration();
        y.set("schema", 1); y.set("match",match); y.set("world",world.getUID().toString());
        List<String> cells = new ArrayList<>();
        for (TntArena.Layer layer : arena.layers()) {
            for (int x = layer.minX(); x <= layer.maxX(); x++) for (int z = layer.minZ(); z <= layer.maxZ(); z++)
                for (int by = layer.topY() - layer.depth() + 1; by <= layer.topY(); by++) {
                    var block = world.getBlockAt(x, by, z);
                    if (block.getState() instanceof TileState) throw new IllegalArgumentException("Tile entity in floor: " + x + "," + by + "," + z);
                    if (!block.getType().isAir() && !arena.materials().contains(block.getType().name()))
                        throw new IllegalArgumentException("Floor material not allowed: " + block.getType());
                    cells.add(x + "," + by + "," + z + "|" + block.getBlockData().getAsString());
                }
        }
        for (TntArena.Point p : arena.spawns())
            if (world.getBlockAt((int)Math.floor(p.x()),(int)Math.floor(p.y())-1,(int)Math.floor(p.z())).getType().isAir()
                    || !world.getBlockAt((int)Math.floor(p.x()),(int)Math.floor(p.y()),(int)Math.floor(p.z())).isPassable()
                    || !world.getBlockAt((int)Math.floor(p.x()),(int)Math.floor(p.y())+1,(int)Math.floor(p.z())).isPassable())
                throw new IllegalArgumentException("Unsafe spawn " + p);
        y.set("cells",cells); y.set("count",cells.size());
        AtomicFiles.write(file, y.saveToString(), false);
    }
    private YamlConfiguration validated() throws Exception {
        YamlConfiguration y = ArenaStore.read(file);
        if (y.getInt("schema") != 1 || y.getString("match","").isBlank() || !y.isList("cells")
                || y.getInt("count") < 1 || y.getInt("count") > 65536 || y.getStringList("cells").size() != y.getInt("count"))
            throw new IllegalStateException("Invalid block journal; retained for operator recovery");
        UUID.fromString(Objects.requireNonNull(y.getString("world")));
        Set<Cell> unique = new HashSet<>();
        for (String row : y.getStringList("cells")) {
            Cell c = cell(row);
            if (!unique.add(c) || row.indexOf('|') < 0 || row.substring(row.indexOf('|')+1).isBlank())
                throw new IllegalArgumentException("Corrupt journal cell");
        }
        return y;
    }
    public boolean restore() throws Exception {
        if (!pending()) return true;
        YamlConfiguration y = validated(); World world = Bukkit.getWorld(UUID.fromString(y.getString("world")));
        if (world == null) return false;
        // Parse all block data first. Corruption must not cause partial writes.
        Map<Cell, org.bukkit.block.data.BlockData> blocks = new LinkedHashMap<>();
        for (String row : y.getStringList("cells")) blocks.put(cell(row),Bukkit.createBlockData(row.substring(row.indexOf('|')+1)));
        for (var entry : blocks.entrySet()) {
            Cell c = entry.getKey(); world.getBlockAt(c.x(),c.y(),c.z()).setBlockData(entry.getValue(),false);
        }
        world.save(); // Flush restored chunks before removing the durable recovery record.
        Files.delete(file); return true;
    }
    private static Cell cell(String row) {
        String[] xyz = row.substring(0,row.indexOf('|')).split(",");
        if (xyz.length != 3) throw new IllegalArgumentException("Corrupt cell coordinates");
        return new Cell(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
    }
}

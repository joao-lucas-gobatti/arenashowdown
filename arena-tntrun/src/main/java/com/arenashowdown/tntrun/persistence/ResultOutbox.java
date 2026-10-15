package com.arenashowdown.tntrun.persistence;

import com.arenashowdown.api.model.*;
import com.arenashowdown.tntrun.arena.ArenaStore;
import org.bukkit.configuration.file.YamlConfiguration;
import java.time.Instant;
import java.nio.file.*;
import java.util.*;

/** One durable result at a time. The stable id makes Core delivery idempotent. */
public final class ResultOutbox {
    private final Path file;
    public ResultOutbox(Path directory) { file = directory.resolve("result.yml"); }
    public boolean pending() { return Files.exists(file); }
    public void save(MatchResult result) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.set("schema",1); y.set("id",result.id()); y.set("match",result.matchId()); y.set("at",result.recordedAt().toString()); y.set("tie",result.tie());
        for (int i = 0; i < result.entries().size(); i++) {
            ResultEntry e = result.entries().get(i); String p = "entries." + i;
            y.set(p+".uuid",e.playerId().toString()); y.set(p+".team",e.teamId()); y.set(p+".position",e.position()); y.set(p+".points",e.points()); y.set(p+".winner",e.winner());
        }
        AtomicFiles.write(file,y.saveToString(),false);
    }
    public MatchResult read() throws Exception {
        YamlConfiguration y = ArenaStore.read(file);
        if (y.getInt("schema") != 1 || y.getString("id","").isBlank() || y.getString("match","").isBlank()
                || y.getConfigurationSection("entries") == null || !(y.get("tie") instanceof Boolean)) throw new IllegalStateException("Corrupt result outbox");
        List<ResultEntry> entries = new ArrayList<>();
        for (String key : y.getConfigurationSection("entries").getKeys(false).stream().sorted(Comparator.comparingInt(Integer::parseInt)).toList()) {
            String p = "entries."+key;
            if (y.getInt(p+".position") != entries.size()+1 || !(y.get(p+".winner") instanceof Boolean)) throw new IllegalStateException("Corrupt standing");
            entries.add(new ResultEntry(UUID.fromString(y.getString(p+".uuid")),y.getString(p+".team"),y.getInt(p+".position"),y.getInt(p+".points"),y.getBoolean(p+".winner")));
        }
        if (entries.isEmpty() || entries.stream().map(ResultEntry::playerId).distinct().count() != entries.size()) throw new IllegalStateException("Invalid result participants");
        return new MatchResult(y.getString("id"),y.getString("match"),entries,y.getBoolean("tie"),Instant.parse(y.getString("at")));
    }
    public void acknowledge() throws Exception { Files.deleteIfExists(file); }
}

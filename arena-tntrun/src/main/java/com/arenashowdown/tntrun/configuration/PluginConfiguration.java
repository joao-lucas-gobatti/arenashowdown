package com.arenashowdown.tntrun.configuration;

import com.arenashowdown.tntrun.ability.AbilityConfig;
import com.arenashowdown.tntrun.arena.*;
import com.arenashowdown.tntrun.persistence.AtomicFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Stages every YAML file before publishing. Match snapshots never consult mutable config. */
public final class PluginConfiguration {
    public record Scoreboard(boolean enabled,int updateTicks,String title,List<String> lines) {
        public Scoreboard { lines=List.copyOf(lines); }
    }
    public record ChampionshipProfile(Map<String,Map<String,Object>> overrides) {
        public ChampionshipProfile {
            Map<String,Map<String,Object>> copy=new LinkedHashMap<>(); overrides.forEach((id,fields) -> copy.put(id,Map.copyOf(fields))); overrides=Map.copyOf(copy);
        }
        public AbilityConfig resolve(AbilityConfig arena) {
            YamlConfiguration yaml=new YamlConfiguration();
            return AbilityConfig.overlay(arena,yaml.createSection("abilities",overrides));
        }
    }
    public record Snapshot(Messages messages,Scoreboard scoreboard,Map<String,ChampionshipProfile> championships) {
        public Snapshot { championships=Map.copyOf(championships); }
    }
    public record MatchSettings(TntArena arena,Messages messages,Scoreboard scoreboard,
                                Collection<TntArena> protectedArenas,AbilityConfig abilities) {
        public MatchSettings { protectedArenas=List.copyOf(protectedArenas); }
    }
    private record Candidate(Map<String,TntArena> arenas,Snapshot settings) {}
    private final Path directory;
    private final ArenaStore arenas;
    private volatile Snapshot current;

    public PluginConfiguration(Path directory,ArenaStore arenas) {
        this.directory=directory; this.arenas=arenas;
        try { current=parseSnapshot(bundled("config.yml"),bundled("messages.yml"),Map.of()); }
        catch (Exception e) { throw new IllegalStateException("Invalid bundled TNT Run configuration",e); }
    }
    public Snapshot current() { return current; }
    public void initialize() throws Exception {
        Files.createDirectories(directory);
        Path config=directory.resolve("config.yml"), messages=directory.resolve("messages.yml");
        if (!Files.exists(config)) AtomicFiles.write(config,bundled("config.yml").saveToString(),false);
        if (!Files.exists(messages)) {
            // One-time migration preserves existing custom inline messages; config.yml stays untouched.
            YamlConfiguration migrated=bundled("messages.yml"), previous=ArenaStore.read(config);
            ConfigurationSection legacy=previous.getConfigurationSection("messages");
            if (legacy!=null) for (String key : legacy.getKeys(true))
                if (!legacy.isConfigurationSection(key) && migrated.contains(key)) migrated.set(key,legacy.get(key));
            for (String key : List.of("scoreboard.title","scoreboard.lines")) if (previous.contains(key)) migrated.set(key,previous.get(key));
            Messages.parse(migrated,bundled("messages.yml"));
            AtomicFiles.write(messages,migrated.saveToString(),false);
        }
        Files.createDirectories(directory.resolve("championships")); reload();
    }
    public void validate() throws Exception { candidate(); }
    public void reload() throws Exception {
        Candidate staged=candidate();
        // All callers publish on the main thread; there is no Bukkit callback between these assignments.
        arenas.publish(staged.arenas()); current=staged.settings();
    }
    private Candidate candidate() throws Exception {
        YamlConfiguration config=ArenaStore.read(directory.resolve("config.yml"));
        YamlConfiguration messages=ArenaStore.read(directory.resolve("messages.yml"));
        Map<String,TntArena> loaded=arenas.readDefinitions();
        Map<String,ChampionshipProfile> profiles=readChampionships();
        Snapshot snapshot=parseSnapshot(config,messages,profiles);
        for (TntArena arena : loaded.values()) for (ChampionshipProfile profile : profiles.values()) profile.resolve(arena.abilities());
        return new Candidate(loaded,snapshot);
    }
    private static Snapshot parseSnapshot(YamlConfiguration config,YamlConfiguration messages,Map<String,ChampionshipProfile> profiles) throws Exception {
        YamlConfiguration defaults=bundled("config.yml"), defaultMessages=bundled("messages.yml");
        Object title=messages.contains("scoreboard.title") ? messages.get("scoreboard.title") : defaultMessages.get("scoreboard.title");
        Object rawLines=messages.contains("scoreboard.lines") ? messages.get("scoreboard.lines") : defaultMessages.get("scoreboard.lines");
        if (!(title instanceof String text) || text.isBlank() || text.length()>128 || !(rawLines instanceof List<?> list)
                || list.isEmpty() || list.size()>15 || list.stream().anyMatch(v -> !(v instanceof String s) || s.length()>256))
            throw new IllegalArgumentException("messages.yml: scoreboard.title must be text and scoreboard.lines must contain 1..15 text lines");
        Object enabled=config.contains("scoreboard.enabled") ? config.get("scoreboard.enabled") : defaults.get("scoreboard.enabled");
        Object ticks=config.contains("scoreboard.update-ticks") ? config.get("scoreboard.update-ticks") : defaults.get("scoreboard.update-ticks");
        if (!(enabled instanceof Boolean on) || !(ticks instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue()!=n.intValue() || n.intValue()<1 || n.intValue()>200)
            throw new IllegalArgumentException("config.yml: scoreboard.enabled must be boolean and update-ticks an integer 1..200");
        for (String key : config.getKeys(false))
            if (!Set.of("scoreboard","messages").contains(key)) throw new IllegalArgumentException("Unknown config.yml section: "+key);
        if (config.contains("scoreboard") && !config.isConfigurationSection("scoreboard")) throw new IllegalArgumentException("scoreboard must be a YAML section");
        if (config.isConfigurationSection("scoreboard")) for (String key : config.getConfigurationSection("scoreboard").getKeys(false))
            if (!Set.of("enabled","update-ticks","title","lines").contains(key)) throw new IllegalArgumentException("Unknown scoreboard setting: "+key);
        if (messages.contains("scoreboard") && !messages.isConfigurationSection("scoreboard")) throw new IllegalArgumentException("messages scoreboard must be a YAML section");
        if (messages.isConfigurationSection("scoreboard")) for (String key : messages.getConfigurationSection("scoreboard").getKeys(false))
            if (!Set.of("title","lines").contains(key)) throw new IllegalArgumentException("Unknown scoreboard message: "+key);
        return new Snapshot(Messages.parse(messages,defaultMessages),new Scoreboard(on,n.intValue(),text,list.stream().map(String.class::cast).toList()),profiles);
    }
    private Map<String,ChampionshipProfile> readChampionships() throws Exception {
        Path folder=directory.resolve("championships"); Map<String,ChampionshipProfile> result=new TreeMap<>();
        if (!Files.exists(folder)) return Map.of();
        try (var files=Files.list(folder)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".yml")).sorted().toList()) {
                String id=file.getFileName().toString().replaceFirst("\\.yml$",""); result.put(id,profile(id,ArenaStore.read(file)));
            }
        }
        return Map.copyOf(result);
    }
    private static ChampionshipProfile profile(String id,YamlConfiguration yaml) {
        if (!id.matches("[a-z0-9_-]{2,64}") || !(yaml.get("id") instanceof String stored) || !id.equals(stored))
            throw new IllegalArgumentException("Championship id must match filename: "+id);
        for (String key : yaml.getKeys(false)) if (!Set.of("id","abilities").contains(key)) throw new IllegalArgumentException("Unknown championship field: "+key);
        ConfigurationSection section=yaml.getConfigurationSection("abilities");
        if (section==null) throw new IllegalArgumentException("Missing championship abilities: "+id);
        AbilityConfig.overlay(AbilityConfig.defaults(),section); Map<String,Map<String,Object>> overrides=new LinkedHashMap<>();
        for (String ability : section.getKeys(false)) overrides.put(ability,section.getConfigurationSection(ability).getValues(false));
        return new ChampionshipProfile(overrides);
    }
    public MatchSettings capture(String arenaId,String championshipId) {
        TntArena arena=arenas.get(arenaId).orElseThrow(() -> new IllegalArgumentException("Unknown arena: "+arenaId)); Snapshot settings=current;
        ChampionshipProfile profile=settings.championships().get(championshipId);
        return new MatchSettings(arena,settings.messages(),settings.scoreboard(),arenas.all(),profile==null ? arena.abilities() : profile.resolve(arena.abilities()));
    }
    private static YamlConfiguration bundled(String name) throws Exception {
        YamlConfiguration yaml=new YamlConfiguration();
        try (var in=Objects.requireNonNull(PluginConfiguration.class.getResourceAsStream("/"+name),"Missing resource "+name)) {
            yaml.loadFromString(new String(in.readAllBytes(),StandardCharsets.UTF_8));
        }
        return yaml;
    }
    public void saveChampionship(String id,String ability,int uses,double strength,int cooldown) throws Exception {
        if (!id.matches("[a-z0-9_-]{2,64}")) throw new IllegalArgumentException("Invalid championship id");
        Path file=directory.resolve("championships").resolve(id+".yml");
        YamlConfiguration yaml=Files.exists(file) ? ArenaStore.read(file) : new YamlConfiguration(); yaml.set("id",id);
        yaml.set("abilities."+ability+".enabled",false); yaml.set("abilities."+ability+".uses",uses);
        yaml.set("abilities."+ability+".strength",strength); yaml.set("abilities."+ability+".cooldown-ticks",cooldown);
        ChampionshipProfile profile=profile(id,yaml); for (TntArena arena : arenas.all()) profile.resolve(arena.abilities());
        Map<String,ChampionshipProfile> next=new TreeMap<>(current.championships()); next.put(id,profile);
        AtomicFiles.write(file,yaml.saveToString(),true); current=new Snapshot(current.messages(),current.scoreboard(),next);
    }
}

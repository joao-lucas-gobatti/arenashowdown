package com.arenashowdown.tntrun.persistence;

import com.arenashowdown.tntrun.arena.ArenaStore;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.*;
import org.bukkit.scoreboard.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import java.nio.file.*;
import java.util.*;

/** Main-thread capture/apply, durable originals, no overwrite of pending snapshots. */
public final class PlayerSnapshots {
    private final Path directory;
    private final Map<UUID, Scoreboard> originalBoards = new HashMap<>();
    public PlayerSnapshots(Path directory) { this.directory = directory; }
    private Path file(UUID id) { return directory.resolve(id + ".yml"); }
    public boolean pending(UUID id) { return Files.exists(file(id)); }
    public boolean anyPending() throws Exception {
        if (!Files.exists(directory)) return false;
        try (var files = Files.list(directory)) { return files.anyMatch(p -> p.toString().endsWith(".yml")); }
    }
    public void capture(Player p) throws Exception {
        if (pending(p.getUniqueId())) throw new IllegalStateException("Player recovery pending: " + p.getUniqueId());
        YamlConfiguration y = new YamlConfiguration(); Location l = p.getLocation();
        y.set("schema",1); y.set("uuid",p.getUniqueId().toString()); y.set("world",p.getWorld().getUID().toString());
        y.set("x",l.getX()); y.set("y",l.getY()); y.set("z",l.getZ()); y.set("yaw",l.getYaw()); y.set("pitch",l.getPitch());
        y.set("mode",p.getGameMode().name()); y.set("health",p.getHealth()); y.set("food",p.getFoodLevel());
        y.set("absorption",p.getAbsorptionAmount());
        y.set("saturation",p.getSaturation()); y.set("exhaustion",p.getExhaustion());
        y.set("level",p.getLevel()); y.set("exp",p.getExp()); y.set("total-exp",p.getTotalExperience());
        y.set("allow-flight",p.getAllowFlight()); y.set("flying",p.isFlying()); y.set("collidable",p.isCollidable());
        y.set("invulnerable",p.isInvulnerable()); y.set("gravity",p.hasGravity());
        y.set("sprinting",p.isSprinting()); y.set("sneaking",p.isSneaking()); y.set("swimming",p.isSwimming()); y.set("gliding",p.isGliding());
        y.set("walk-speed",p.getWalkSpeed()); y.set("fly-speed",p.getFlySpeed());
        y.set("fire",p.getFireTicks()); y.set("freeze",p.getFreezeTicks()); y.set("fall",p.getFallDistance());
        y.set("velocity",List.of(p.getVelocity().getX(),p.getVelocity().getY(),p.getVelocity().getZ()));
        y.set("inventory",Arrays.stream(p.getInventory().getContents()).map(i -> i == null ? "_" : Base64.getEncoder().encodeToString(i.serializeAsBytes())).toList());
        ItemStack cursor=p.getItemOnCursor(); y.set("cursor",cursor == null ? "_" : Base64.getEncoder().encodeToString(cursor.serializeAsBytes()));
        y.set("held-slot",p.getInventory().getHeldItemSlot());
        List<String> effects = p.getActivePotionEffects().stream().map(e -> e.getType().getKey()+","+e.getDuration()+","+e.getAmplifier()+","+e.isAmbient()+","+e.hasParticles()+","+e.hasIcon()).toList();
        y.set("effects",effects);
        saveBoard(y,p.getScoreboard());
        AtomicFiles.write(file(p.getUniqueId()),y.saveToString(),false);
        originalBoards.put(p.getUniqueId(),p.getScoreboard());
    }
    public boolean restore(Player p) throws Exception {
        if (!pending(p.getUniqueId())) return true;
        YamlConfiguration y = ArenaStore.read(file(p.getUniqueId()));
        validateNumbers(y);
        if (!p.getUniqueId().toString().equals(y.getString("uuid"))) throw new IllegalStateException("Invalid player snapshot");
        for (String key : List.of("allow-flight","flying","collidable","invulnerable","gravity","sprinting","sneaking","swimming","gliding","board.main"))
            if (!(y.get(key) instanceof Boolean)) throw new IllegalStateException("Missing snapshot boolean: " + key);
        if (!y.isList("inventory") || !y.isList("effects") || y.getStringList("inventory").size() != p.getInventory().getSize())
            throw new IllegalStateException("Truncated snapshot");
        World w = Bukkit.getWorld(UUID.fromString(y.getString("world")));
        if (w == null) return false;
        GameMode mode = GameMode.valueOf(y.getString("mode"));
        ItemStack[] inventory = y.getStringList("inventory").stream().map(s -> s.equals("_") ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(s))).toArray(ItemStack[]::new);
        String storedCursor=Objects.requireNonNull(y.getString("cursor"),"Missing cursor snapshot");
        ItemStack cursor=storedCursor.equals("_") ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(storedCursor));
        List<PotionEffect> effects = new ArrayList<>();
        for (String s : y.getStringList("effects")) {
            String[] v = s.split(","); if (v.length != 6) throw new IllegalStateException("Corrupt potion snapshot");
            PotionEffectType type = Registry.EFFECT.get(Objects.requireNonNull(NamespacedKey.fromString(v[0])));
            effects.add(new PotionEffect(Objects.requireNonNull(type),Integer.parseInt(v[1]),Integer.parseInt(v[2]),Boolean.parseBoolean(v[3]),Boolean.parseBoolean(v[4]),Boolean.parseBoolean(v[5])));
        }
        List<?> storedVelocity = y.getList("velocity");
        if (storedVelocity == null || storedVelocity.size() != 3 || storedVelocity.stream().anyMatch(v -> !(v instanceof Number n) || !Double.isFinite(n.doubleValue())))
            throw new IllegalStateException("Corrupt velocity");
        List<Double> velocity = storedVelocity.stream().map(v -> ((Number)v).doubleValue()).toList();
        Scoreboard board = originalBoards.get(p.getUniqueId());
        if (board == null) board = loadBoard(y);
        if (!p.teleport(new Location(w,y.getDouble("x"),y.getDouble("y"),y.getDouble("z"),(float)y.getDouble("yaw"),(float)y.getDouble("pitch"))))
            return false;
        p.closeInventory(); p.setGameMode(mode); p.getInventory().setContents(inventory); p.getInventory().setHeldItemSlot(y.getInt("held-slot")); p.setItemOnCursor(cursor);
        // Effects (e.g. HEALTH_BOOST) may change the maximum health used for clamping.
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType())); p.addPotionEffects(effects);
        p.setHealth(Math.min(y.getDouble("health"),p.getMaxHealth())); p.setFoodLevel(y.getInt("food"));
        p.setAbsorptionAmount(y.getDouble("absorption"));
        p.setSaturation((float)y.getDouble("saturation")); p.setExhaustion((float)y.getDouble("exhaustion"));
        p.setLevel(y.getInt("level")); p.setExp((float)y.getDouble("exp")); p.setTotalExperience(y.getInt("total-exp"));
        p.setAllowFlight(y.getBoolean("allow-flight")); p.setFlying(y.getBoolean("flying"));
        p.setCollidable(y.getBoolean("collidable")); p.setInvulnerable(y.getBoolean("invulnerable")); p.setGravity(y.getBoolean("gravity"));
        p.setSprinting(y.getBoolean("sprinting")); p.setSneaking(y.getBoolean("sneaking")); p.setSwimming(y.getBoolean("swimming")); p.setGliding(y.getBoolean("gliding"));
        p.setWalkSpeed((float)y.getDouble("walk-speed")); p.setFlySpeed((float)y.getDouble("fly-speed"));
        p.setFireTicks(y.getInt("fire")); p.setFreezeTicks(y.getInt("freeze")); p.setFallDistance((float)y.getDouble("fall"));
        p.setVelocity(new Vector(velocity.get(0),velocity.get(1),velocity.get(2)));
        p.setScoreboard(board);
        p.saveData(); // Durable inventory/state before consuming the recovery file.
        Files.delete(file(p.getUniqueId())); originalBoards.remove(p.getUniqueId()); return true;
    }
    /** Validate setter contracts before teleport, inventory replacement or any other player mutation. */
    private static void validateNumbers(YamlConfiguration y) {
        integer(y,"schema",1,1);
        for (String key : List.of("x","y","z")) number(y,key);
        for (String key : List.of("yaw","pitch","health","absorption","saturation","exhaustion","exp","walk-speed","fly-speed","fall"))
            if (!Float.isFinite((float)number(y,key))) throw new IllegalStateException("Invalid snapshot float: " + key);
        integer(y,"food",Integer.MIN_VALUE,Integer.MAX_VALUE);
        integer(y,"level",0,Integer.MAX_VALUE); integer(y,"total-exp",0,Integer.MAX_VALUE);
        integer(y,"held-slot",0,8); integer(y,"freeze",0,Integer.MAX_VALUE);
        // Negative fire ticks are a valid Bukkit state; do not clamp captured originals.
        integer(y,"fire",Integer.MIN_VALUE,Integer.MAX_VALUE);
        range(y,"exp",0,1); range(y,"walk-speed",-1,1); range(y,"fly-speed",-1,1);
        if ((float)y.getDouble("health") <= 0 || y.getDouble("absorption") < 0)
            throw new IllegalStateException("Invalid snapshot health/absorption");
    }
    private static double number(YamlConfiguration y,String key) {
        if (!(y.get(key) instanceof Number n) || !Double.isFinite(n.doubleValue()))
            throw new IllegalStateException("Invalid snapshot field: " + key);
        return n.doubleValue();
    }
    private static void integer(YamlConfiguration y,String key,int min,int max) {
        double value=number(y,key);
        if (value != Math.rint(value) || value < min || value > max)
            throw new IllegalStateException("Invalid snapshot integer: " + key);
    }
    private static void range(YamlConfiguration y,String key,double min,double max) {
        double value=number(y,key);
        if (value < min || value > max) throw new IllegalStateException("Invalid snapshot range: " + key);
    }
    private static String json(net.kyori.adventure.text.Component c) { return GsonComponentSerializer.gson().serialize(c); }
    private static net.kyori.adventure.text.Component component(String s) { return GsonComponentSerializer.gson().deserialize(Objects.requireNonNull(s)); }
    private static void saveBoard(YamlConfiguration y, Scoreboard board) {
        boolean main = board == Bukkit.getScoreboardManager().getMainScoreboard(); y.set("board.main",main); if (main) return;
        int i = 0;
        for (Objective o : board.getObjectives()) {
            String path = "board.objectives." + i++;
            y.set(path+".name",o.getName()); y.set(path+".criteria",o.getCriteria()); y.set(path+".title",json(o.displayName()));
            y.set(path+".render",o.getRenderType().name()); y.set(path+".slot",o.getDisplaySlot() == null ? null : o.getDisplaySlot().name());
            int j = 0;
            for (String entry : board.getEntries()) if (o.getScore(entry).isScoreSet()) {
                y.set(path+".scores."+j+".entry",entry); y.set(path+".scores."+j+++".value",o.getScore(entry).getScore());
            }
        }
        i = 0;
        for (Team team : board.getTeams()) {
            String path = "board.teams." + i++;
            y.set(path+".name",team.getName()); y.set(path+".title",json(team.displayName())); y.set(path+".prefix",json(team.prefix())); y.set(path+".suffix",json(team.suffix()));
            y.set(path+".color",team.getColor().name()); y.set(path+".friendly-fire",team.allowFriendlyFire()); y.set(path+".invisibles",team.canSeeFriendlyInvisibles());
            y.set(path+".entries",new ArrayList<>(team.getEntries()));
            for (Team.Option option : Team.Option.values()) y.set(path+".options."+option.name(),team.getOption(option).name());
        }
    }
    private static Scoreboard loadBoard(YamlConfiguration y) {
        if (y.getBoolean("board.main")) return Bukkit.getScoreboardManager().getMainScoreboard();
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        var objectives = y.getConfigurationSection("board.objectives");
        if (objectives != null) for (String key : objectives.getKeys(false)) {
            var s = objectives.getConfigurationSection(key);
            Objective o = board.registerNewObjective(s.getString("name"),Criteria.create(s.getString("criteria")),component(s.getString("title")),RenderType.valueOf(s.getString("render")));
            if (s.getString("slot") != null) o.setDisplaySlot(DisplaySlot.valueOf(s.getString("slot")));
            var scores = s.getConfigurationSection("scores"); if (scores != null && o.isModifiable()) for (String k : scores.getKeys(false)) o.getScore(scores.getString(k+".entry")).setScore(scores.getInt(k+".value"));
        }
        var teams = y.getConfigurationSection("board.teams");
        if (teams != null) for (String key : teams.getKeys(false)) {
            var s = teams.getConfigurationSection(key); Team team = board.registerNewTeam(s.getString("name"));
            team.displayName(component(s.getString("title"))); team.prefix(component(s.getString("prefix"))); team.suffix(component(s.getString("suffix")));
            team.setColor(ChatColor.valueOf(s.getString("color"))); team.setAllowFriendlyFire(s.getBoolean("friendly-fire")); team.setCanSeeFriendlyInvisibles(s.getBoolean("invisibles"));
            for (Team.Option option : Team.Option.values()) team.setOption(option,Team.OptionStatus.valueOf(s.getString("options."+option.name())));
            s.getStringList("entries").forEach(team::addEntry);
        }
        return board;
    }
}

package com.arenashowdown.tntrun.verification;

import com.arenashowdown.api.OrchestrationApi;
import com.arenashowdown.tntrun.arena.*;
import com.arenashowdown.tntrun.persistence.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiFunction;

/** Executable verification on REAL Paper; refuses every non-isolated environment. */
public final class PaperVerificationPlugin extends JavaPlugin {
    @Override public void onEnable() {
        Objects.requireNonNull(getCommand("tntrunverify")).setExecutor((sender,c,label,args) -> {
            try {
                Path root=getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
                if (!root.endsWith(Path.of("arena-tntrun",".test-server")) || !"true".equals(System.getProperty("arenatntrun.isolated")))
                    throw new IllegalStateException("Verification requires arena-tntrun/.test-server and -Darenatntrun.isolated=true");
                World world=Objects.requireNonNull(Bukkit.getWorld("tntrun_test"));
                if (!getDataFolder().toPath().startsWith(root) && !getDataFolder().toPath().toAbsolutePath().startsWith(root)) throw new IllegalStateException("Non-isolated plugin folder");
                require(Bukkit.getServicesManager().load(OrchestrationApi.class)!=null,"OrchestrationApi missing");
                verifyBlocks(world); verifyPlayer(world);
                Files.writeString(getDataFolder().toPath().resolve("verification.ok"),"Paper 1.21.11: blocks, atomic recovery, binary inventory, potion effects, scoreboard, player states OK\n");
                getLogger().info("TNTRUN_PAPER_VERIFICATION_OK"); sender.sendMessage(Component.text("TNTRUN_PAPER_VERIFICATION_OK"));
            } catch (Exception e) { getLogger().log(java.util.logging.Level.SEVERE,"TNTRUN_PAPER_VERIFICATION_FAILED",e); }
            return true;
        });
    }
    private void verifyBlocks(World world) throws Exception {
        Path dir=getDataFolder().toPath().resolve("block-test");
        TntArena arena;
        try (var in=Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("ArenaTNTRun")).getResource("arena.example.yml")) {
            var yaml=new org.bukkit.configuration.file.YamlConfiguration(); yaml.loadFromString(new String(Objects.requireNonNull(in).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)); arena=ArenaStore.parse(yaml);
        }
        for (var l : arena.layers()) for (int x=l.minX(); x<=l.maxX(); x++) for (int z=l.minZ(); z<=l.maxZ(); z++)
            for (int y=l.topY()-l.depth()+1; y<=l.topY(); y++) world.getBlockAt(x,y,z).setType(y==l.topY() ? Material.SAND : Material.TNT,false);
        BlockJournal journal=new BlockJournal(dir); journal.capture(arena,world,"paper-block-test");
        world.getBlockAt(3,75,3).setType(Material.AIR,false); world.getBlockAt(3,74,3).setType(Material.AIR,false);
        require(new BlockJournal(dir).restore(),"Block recovery failed");
        require(world.getBlockAt(3,75,3).getType()==Material.SAND && world.getBlockAt(3,74,3).getType()==Material.TNT,"Original floors not restored");
        require(!journal.pending(),"Block journal not consumed");
    }
    private void verifyPlayer(World world) throws Exception {
        TestPlayer fake=new TestPlayer(world); Player p=fake.player; Path dir=getDataFolder().toPath().resolve("player-test");
        ItemStack sword=ItemStack.of(Material.DIAMOND_SWORD); sword.editMeta(m -> { m.displayName(Component.text("TNT Run recovery")); m.addEnchant(Enchantment.UNBREAKING,3,true); });
        fake.items[0]=sword; fake.items[40]=ItemStack.of(Material.SHIELD);
        ItemStack cursor=ItemStack.of(Material.ENDER_PEARL,4); fake.state.put("ItemOnCursor",cursor);
        Scoreboard original=Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective=original.registerNewObjective("before",Criteria.DUMMY,Component.text("Original board")); objective.setDisplaySlot(DisplaySlot.SIDEBAR); objective.getScore("original entry").setScore(42);
        Team team=original.registerNewTeam("original"); team.prefix(Component.text("prefix")); team.suffix(Component.text("suffix")); team.setColor(ChatColor.GOLD); team.addEntry("entry"); team.setOption(Team.Option.COLLISION_RULE,Team.OptionStatus.NEVER);
        fake.state.put("Scoreboard",original); Location originalLocation=p.getLocation();
        PlayerSnapshots snapshots=new PlayerSnapshots(dir); snapshots.capture(p);
        p.getInventory().clear(); p.setGameMode(GameMode.SPECTATOR); p.teleport(new Location(world,8,82,8)); p.setHealth(2); p.setAllowFlight(false); p.setFoodLevel(1);
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); p.setItemOnCursor(null); fake.effects.clear();
        p.setAbsorptionAmount(0); p.setHealth(2);
        // New instance forces durable reload, including a reconstructed custom scoreboard.
        require(new PlayerSnapshots(dir).restore(p),"Player restore failed");
        require(sword.equals(fake.items[0]) && fake.items[40].getType()==Material.SHIELD,"Binary inventory metadata/offhand mismatch");
        require(cursor.equals(p.getItemOnCursor()),"Cursor item lost");
        require(p.getLocation().equals(originalLocation) && p.getGameMode()==GameMode.CREATIVE && p.getHealth()==25 && p.getFoodLevel()==13 && p.getAllowFlight(),"Player scalar state/health boost mismatch");
        require(p.getScoreboard().getObjective("before").getScore("original entry").getScore()==42,"Objective/score lost after recovery");
        require(p.getScoreboard().getTeam("original").getOption(Team.Option.COLLISION_RULE)==Team.OptionStatus.NEVER,"Team option lost");
        require(fake.effects.size()==2 && fake.effects.stream().anyMatch(e -> e.getType()==PotionEffectType.SPEED) && fake.effects.stream().anyMatch(e -> e.getType()==PotionEffectType.HEALTH_BOOST),"Potion state lost");
        require(p.getAbsorptionAmount()==5,"Absorption state lost");
        require(fake.saved && !snapshots.pending(p.getUniqueId()),"Player persistence not flushed before journal removal");
    }
    private static void require(boolean ok,String message) { if (!ok) throw new IllegalStateException(message); }
    private static final class TestPlayer {
        final Map<String,Object> state=new HashMap<>();
        final List<PotionEffect> effects=new ArrayList<>(List.of(new PotionEffect(PotionEffectType.SPEED,200,1,true,false,true),new PotionEffect(PotionEffectType.HEALTH_BOOST,200,1,false,true,true)));
        ItemStack[] items=new ItemStack[41]; Location location; boolean saved;
        final Player player;
        TestPlayer(World world) {
            location=new Location(world,-10,90,-10);
            state.put("GameMode",GameMode.CREATIVE); state.put("Health",25d); state.put("FoodLevel",13); state.put("Saturation",4f); state.put("Exhaustion",1f);
            state.put("AbsorptionAmount",5d); state.put("Sprinting",true); state.put("Sneaking",false); state.put("Swimming",false); state.put("Gliding",false);
            state.put("Level",7); state.put("Exp",.3f); state.put("TotalExperience",99); state.put("AllowFlight",true); state.put("Flying",false);
            state.put("Collidable",true); state.put("Invulnerable",false); state.put("Gravity",true); state.put("WalkSpeed",.3f); state.put("FlySpeed",.2f);
            state.put("FireTicks",10); state.put("FreezeTicks",5); state.put("FallDistance",2f); state.put("Velocity",new Vector(.1,.2,.3));
            PlayerInventory inventory=proxy(PlayerInventory.class,(m,a) -> switch(m) {
                case "getContents" -> items.clone(); case "setContents" -> { items=((ItemStack[])a[0]).clone(); yield null; }
                case "getSize" -> 41; case "getHeldItemSlot" -> state.getOrDefault("held",2); case "setHeldItemSlot" -> { state.put("held",a[0]); yield null; }
                case "clear" -> { items=new ItemStack[41]; yield null; } default -> null;
            });
            player=proxy(Player.class,(m,a) -> switch(m) {
                case "getUniqueId" -> new UUID(0,999); case "getName" -> "PaperVerification"; case "getLocation" -> location.clone(); case "getWorld" -> world;
                case "getMaxHealth" -> 20d+(effects.stream().anyMatch(e -> e.getType()==PotionEffectType.HEALTH_BOOST) ? 8 : 0); case "getInventory" -> inventory; case "getActivePotionEffects" -> List.copyOf(effects);
                case "hasGravity" -> state.get("Gravity"); case "teleport" -> { location=((Location)a[0]).clone(); yield true; }
                case "saveData" -> { saved=true; yield null; } case "removePotionEffect" -> { effects.removeIf(e -> e.getType()==a[0]); yield null; }
                case "addPotionEffects" -> { for (Object e : (Collection<?>)a[0]) effects.add((PotionEffect)e); yield true; }
                default -> {
                    if (m.startsWith("set") && a!=null && a.length==1) { state.put(m.substring(3),a[0]); yield null; }
                    if (m.startsWith("get") && state.containsKey(m.substring(3))) yield state.get(m.substring(3));
                    if (m.startsWith("is") && state.containsKey(m.substring(2))) yield state.get(m.substring(2)); yield null;
                }
            });
        }
    }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type,BiFunction<String,Object[],Object> fn) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a) -> {
            if (m.getName().equals("equals")) return p==a[0]; if (m.getName().equals("hashCode")) return System.identityHashCode(p); if (m.getName().equals("toString")) return "VerificationPlayer";
            Object value=fn.apply(m.getName(),a);
            if (value!=null || !m.getReturnType().isPrimitive() || m.getReturnType()==void.class) return value;
            if (m.getReturnType()==boolean.class) return false; if (m.getReturnType()==float.class) return 0f; if (m.getReturnType()==double.class) return 0d; if (m.getReturnType()==long.class) return 0L; return 0;
        });
    }
}

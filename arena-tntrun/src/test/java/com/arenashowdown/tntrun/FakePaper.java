package com.arenashowdown.tntrun;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.*;
import org.bukkit.scoreboard.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/** Deterministic main-thread Bukkit boundary; no worlds, network or databases on disk. */
final class FakePaper {
    static FakePaper current;
    static boolean installed;
    final UUID worldId=new UUID(5,1);
    final Map<UUID,Human> humans=new LinkedHashMap<>();
    final Map<String,String> cells=new HashMap<>();
    final ArrayDeque<Runnable> callbacks=new ArrayDeque<>();
    final List<Task> timers=new ArrayList<>();
    final Scoreboard main=board();
    final ScoreboardManager manager=proxy(ScoreboardManager.class,(m,a) -> m.equals("getMainScoreboard") ? main : board());
    boolean worldLoaded=true;
    int saves;
    final World world=proxy(World.class,(m,a) -> switch(m) {
        case "getUID" -> worldId;
        case "getName" -> "tntrun_test";
        case "getMinHeight" -> -64;
        case "getMaxHeight" -> 320;
        case "getPlayers" -> humans.values().stream().filter(h -> h.online && h.location.getWorld().equals(world())).map(h -> h.player).toList();
        case "getBlockAt" -> block((Integer)a[0],(Integer)a[1],(Integer)a[2]);
        case "save" -> { saves++; yield null; }
        default -> fallback(m);
    });
    final BukkitScheduler scheduler=proxy(BukkitScheduler.class,(m,a) -> {
        if (m.equals("runTask")) { callbacks.add((Runnable)a[1]); return new Task((Runnable)a[1]).handle; }
        if (m.equals("runTaskTimer")) { Task t=new Task((Runnable)a[1]); timers.add(t); return t.handle; }
        return fallback(m);
    });
    FakePaper() {
        current=this;
        if (!installed) {
            Server server=proxy(Server.class,(m,a) -> switch(m) {
                case "getLogger" -> Logger.getLogger("FakePaper");
                case "getName","getVersion","getBukkitVersion" -> "FakePaper-1.21.11";
                case "isPrimaryThread" -> true;
                case "getScheduler" -> current.scheduler;
                case "getScoreboardManager" -> current.manager;
                case "getScoreboardCriteria" -> proxy(Criteria.class,(name,args) -> name.equals("getName") ? a[0] : name.equals("getDefaultRenderType") ? RenderType.INTEGER : false);
                case "getWorld" -> current.worldLoaded ? current.world : null;
                case "getPlayer" -> { Human h=current.humans.get(a[0]); yield h!=null && h.online ? h.player : null; }
                case "getOnlinePlayers" -> current.humans.values().stream().filter(h -> h.online).map(h -> h.player).toList();
                case "createBlockData" -> data((String)a[0]);
                default -> fallback(m);
            });
            try { Bukkit.setServer(server); }
            catch (NoSuchElementException e) {
                // API-only JVM has no ServerBuildInfo service for Bukkit's startup log.
                if (Bukkit.getServer()!=server) throw e;
            }
            installed=true;
        }
    }
    World world() { return world; }
    Plugin plugin(Path folder) throws Exception {
        YamlConfiguration config=new YamlConfiguration();
        try (var in=getClass().getResourceAsStream("/config.yml")) { config.loadFromString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)); }
        return proxy(Plugin.class,(m,a) -> switch(m) {
            case "getDataFolder" -> folder.toFile();
            case "isEnabled" -> true;
            case "getConfig" -> config;
            case "getLogger" -> Logger.getLogger("TNT-Test");
            case "getServer" -> Bukkit.getServer();
            case "getName" -> "ArenaTNTRun";
            default -> fallback(m);
        });
    }
    Human human(int id) {
        Human h=new Human(new UUID(0,id)); humans.put(h.id,h); return h;
    }
    void floors(com.arenashowdown.tntrun.arena.TntArena arena) {
        for (var l : arena.layers()) for (int x=l.minX(); x<=l.maxX(); x++) for (int z=l.minZ(); z<=l.maxZ(); z++)
            for (int y=l.topY()-l.depth()+1; y<=l.topY(); y++) cells.put(key(x,y,z),y==l.topY() ? "minecraft:sand" : "minecraft:tnt");
    }
    void flush() { for (int limit=0; !callbacks.isEmpty() && limit<1000; limit++) callbacks.remove().run(); if (!callbacks.isEmpty()) throw new AssertionError("Callback loop"); }
    void ticks(int count) { for (int i=0;i<count;i++) { flush(); for (Task t : List.copyOf(timers)) if (!t.cancelled) t.action.run(); flush(); } }
    String at(int x,int y,int z) { return cells.getOrDefault(key(x,y,z),"minecraft:air"); }
    private static String key(int x,int y,int z) { return x+","+y+","+z; }
    private Block block(int x,int y,int z) {
        return proxy(Block.class,(m,a) -> switch(m) {
            case "getType" -> Material.valueOf(at(x,y,z).substring(10).toUpperCase(Locale.ROOT));
            case "getBlockData" -> data(at(x,y,z));
            case "isPassable", "isEmpty" -> at(x,y,z).equals("minecraft:air");
            case "getLocation" -> new Location(world,x,y,z);
            case "setBlockData" -> { if (!Boolean.FALSE.equals(a[1])) throw new AssertionError("Physics must be disabled"); cells.put(key(x,y,z),((BlockData)a[0]).getAsString()); yield null; }
            case "setType" -> { if (!Boolean.FALSE.equals(a[1])) throw new AssertionError("Physics must be disabled"); cells.put(key(x,y,z),"minecraft:"+((Material)a[0]).name().toLowerCase(Locale.ROOT)); yield null; }
            default -> fallback(m);
        });
    }
    private static BlockData data(String value) { return proxy(BlockData.class,(m,a) -> m.equals("getAsString") ? value : fallback(m)); }
    private static Scoreboard board() {
        Map<String,Objective> objectives=new HashMap<>(); Set<String> entries=new HashSet<>();
        return proxy(Scoreboard.class,(m,a) -> switch(m) {
            case "getObjectives" -> new HashSet<>(objectives.values());
            case "getTeams" -> Set.of();
            case "getEntries" -> new HashSet<>(entries);
            case "resetScores" -> { entries.remove(a[0]); yield null; }
            case "getObjective" -> objectives.get(a[0]);
            case "registerNewObjective" -> {
                String name=(String)a[0]; Map<String,Integer> scores=new HashMap<>();
                Objective o=proxy(Objective.class,(om,oa) -> switch(om) {
                    case "getName" -> name;
                    case "getCriteria" -> "dummy";
                    case "displayName" -> Component.text(name);
                    case "getRenderType" -> RenderType.INTEGER;
                    case "isModifiable" -> true;
                    case "getScore" -> proxy(Score.class,(sm,sa) -> switch(sm) {
                        case "setScore" -> { entries.add((String)oa[0]); scores.put((String)oa[0],(Integer)sa[0]); yield null; }
                        case "getScore" -> scores.getOrDefault(oa[0],0);
                        case "isScoreSet" -> scores.containsKey(oa[0]);
                        default -> fallback(sm);
                    });
                    default -> fallback(om);
                }); objectives.put(name,o); yield o;
            }
            default -> fallback(m);
        });
    }
    final class Human {
        final UUID id;
        final Player player;
        Location location=new Location(world,-10,90,-10);
        boolean online=true;
        int saved, teleports;
        ItemStack[] items=new ItemStack[41];
        final Map<String,Object> state=new HashMap<>();
        final List<Component> messages=new ArrayList<>();
        Human(UUID id) {
            this.id=id;
            state.put("GameMode",GameMode.CREATIVE); state.put("Health",17d); state.put("FoodLevel",13); state.put("Saturation",4f); state.put("Exhaustion",1f);
            state.put("AbsorptionAmount",5d); state.put("Sprinting",true); state.put("Sneaking",true); state.put("Swimming",false); state.put("Gliding",false);
            state.put("Level",7); state.put("Exp",.3f); state.put("TotalExperience",99); state.put("AllowFlight",true); state.put("Flying",true);
            state.put("Collidable",true); state.put("Invulnerable",false); state.put("Gravity",true); state.put("WalkSpeed",.3f); state.put("FlySpeed",.2f);
            state.put("FireTicks",10); state.put("FreezeTicks",5); state.put("FallDistance",2f); state.put("Velocity",new Vector(.1,.2,.3)); state.put("Scoreboard",main);
            PlayerInventory inventory=proxy(PlayerInventory.class,(m,a) -> switch(m) {
                case "getContents" -> items.clone();
                case "setContents" -> { items=((ItemStack[])a[0]).clone(); yield null; }
                case "getSize" -> 41;
                case "getHeldItemSlot" -> state.getOrDefault("held",2);
                case "setHeldItemSlot" -> { state.put("held",a[0]); yield null; }
                case "clear" -> { items=new ItemStack[41]; yield null; }
                default -> fallback(m);
            });
            player=proxy(Player.class,(m,a) -> switch(m) {
                case "getUniqueId" -> id;
                case "getName" -> "Player"+id.getLeastSignificantBits();
                case "getLocation" -> location.clone();
                case "getWorld" -> location.getWorld();
                case "isOnline" -> online;
                case "isDead","isGliding" -> false;
                case "getMaxHealth" -> 20d;
                case "getInventory" -> inventory;
                case "hasPermission" -> state.getOrDefault("Permission",false);
                case "getActivePotionEffects" -> List.of();
                case "hasGravity" -> state.get("Gravity");
                case "teleport" -> { location=((Location)a[0]).clone(); teleports++; yield true; }
                case "saveData" -> { saved++; yield null; }
                case "sendMessage" -> { if (a[0] instanceof Component c) messages.add(c); yield null; }
                case "kick" -> { online=false; yield null; }
                default -> {
                    if (m.startsWith("set") && a!=null && a.length==1) { state.put(m.substring(3),a[0]); yield null; }
                    if (m.startsWith("get") && state.containsKey(m.substring(3))) yield state.get(m.substring(3));
                    if (m.startsWith("is") && state.containsKey(m.substring(2))) yield state.get(m.substring(2));
                    yield fallback(m);
                }
            });
        }
    }
    final class Task {
        final Runnable action; boolean cancelled;
        final BukkitTask handle=proxy(BukkitTask.class,(m,a) -> {
            if (m.equals("cancel")) { cancelled=true; return null; } if (m.equals("isCancelled")) return cancelled; return fallback(m);
        });
        Task(Runnable action) { this.action=action; }
    }
    private static Object fallback(String name) { return null; }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type,BiFunction<String,Object[],Object> handler) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a) -> {
            if (m.getName().equals("equals")) return p==a[0]; if (m.getName().equals("hashCode")) return System.identityHashCode(p); if (m.getName().equals("toString")) return "Fake"+type.getSimpleName();
            Object result=handler.apply(m.getName(),a);
            if (result!=null || !m.getReturnType().isPrimitive() || m.getReturnType()==void.class) return result;
            if (m.getReturnType()==boolean.class) return false; if (m.getReturnType()==long.class) return 0L; if (m.getReturnType()==double.class) return 0d; if (m.getReturnType()==float.class) return 0f;
            return 0;
        });
    }
}

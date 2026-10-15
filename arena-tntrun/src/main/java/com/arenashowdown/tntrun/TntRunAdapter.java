package com.arenashowdown.tntrun;

import com.arenashowdown.api.OrchestrationApi;
import com.arenashowdown.api.minigame.*;
import com.arenashowdown.api.model.*;
import com.arenashowdown.tntrun.arena.*;
import com.arenashowdown.tntrun.game.*;
import com.arenashowdown.tntrun.persistence.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.nio.file.*;

public final class TntRunAdapter implements MinigameAdapter {
    private enum Phase { PREPARING, COUNTDOWN, RUNNING, ENDING }
    private final Plugin plugin;
    private final OrchestrationApi api;
    private final ArenaStore arenas;
    private final BlockJournal blocks;
    private final PlayerSnapshots snapshots;
    private final ResultOutbox outbox;
    private final Map<UUID,String> teams = new HashMap<>();
    private final Set<UUID> participants = new LinkedHashSet<>(), viewers = new HashSet<>();
    private final RemovalQueue<BlockJournal.Cell> removals = new RemovalQueue<>();
    private volatile ArenaMatch match;
    private TntArena arena;
    private SurvivalRound round;
    private MatchResult result;
    private Phase phase;
    private BukkitTask task;
    private long generation;
    private int countdownTicks;
    private boolean restoring, delivering, startRequested, internalMove;
    private volatile String interruptedMatch;
    private final Path session;

    public TntRunAdapter(Plugin plugin, OrchestrationApi api, ArenaStore arenas,
                         BlockJournal blocks, PlayerSnapshots snapshots, ResultOutbox outbox) {
        this.plugin=plugin; this.api=api; this.arenas=arenas; this.blocks=blocks; this.snapshots=snapshots; this.outbox=outbox;
        session=plugin.getDataFolder().toPath().resolve("recovery/session.txt");
    }
    @Override public String id() { return "tntrun"; }
    @Override public boolean supportsArena(String id) {
        // Called by Core off-thread: only immutable arena metadata and durable/busy flags.
        return match == null && !blocks.pending() && !outbox.pending() && !Files.exists(session) && interruptedMatch == null && arenas.get(id).isPresent();
    }
    public boolean busy() { return match != null || blocks.pending() || outbox.pending() || Files.exists(session) || interruptedMatch != null; }
    public String status() { return match == null ? (busy() ? "RECOVERY_PENDING" : "IDLE") : match.id()+" / "+phase; }
    @Override public void prepare(MinigameContext context) {
        mainThread();
        if (busy()) throw new IllegalStateException("ArenaTNTRun busy/recovery pending; only one match at a time");
        match = context.match(); phase = Phase.PREPARING; long token = ++generation;
        try {
            if (!"tntrun".equals(match.minigame()) || match.state() != MatchState.PREPARING) throw new IllegalArgumentException("Invalid TNT Run context");
            arena = arenas.get(match.arena()).orElseThrow(() -> new IllegalArgumentException("Unknown arena: " + match.arena()));
            World world = Objects.requireNonNull(Bukkit.getWorld(arena.world()),"Arena world not loaded");
            if (arena.bounds().minY() < world.getMinHeight() || arena.bounds().maxY() >= world.getMaxHeight()) throw new IllegalArgumentException("Bounds outside world height");
            round = new SurvivalRound(match.participants()); participants.addAll(match.participants());
            for (Player visitor : world.getPlayers())
                if (!participants.contains(visitor.getUniqueId()) && inside(visitor.getLocation())) throw new IllegalArgumentException("Nonparticipant inside arena: "+visitor.getName());
            if (participants.size() > arena.spawns().size()) throw new IllegalArgumentException("Not enough spawns");
            for (UUID id : participants) {
                Player p = Objects.requireNonNull(Bukkit.getPlayer(id),"Offline participant " + id);
                if (p.isDead() || p.isInsideVehicle() || p.isSleeping() || snapshots.pending(id)) throw new IllegalArgumentException("Player unavailable/recovery pending " + id);
            }
            AtomicFiles.write(session,match.id(),false);
            api.participants(match.championshipId()).orTimeout(15,TimeUnit.SECONDS).whenComplete((infos,error) -> onMain(() -> {
                if (token != generation || match == null) return;
                if (error != null) { fail("Teams: " + root(error)); return; }
                try {
                    infos.forEach(info -> teams.put(info.playerId(),info.teamId()));
                    blocks.capture(arena,world,match.id());
                    // All originals must be durable before the first player is changed.
                    for (UUID id : participants) snapshots.capture(Objects.requireNonNull(Bukkit.getPlayer(id),"Participant disconnected"));
                    int i = 0;
                    for (UUID id : participants) {
                        Player p = Objects.requireNonNull(Bukkit.getPlayer(id)); normalize(p);
                        teleport(p,arena.spawns().get(i++));
                    }
                    api.transitionMatch(match.id(),MatchState.COUNTDOWN,"ArenaTNTRun",null).orTimeout(15,TimeUnit.SECONDS)
                        .whenComplete((m,e) -> onMain(() -> {
                            if (token != generation || match == null) return;
                            if (e != null) { fail("Countdown: " + root(e)); return; }
                            phase = Phase.COUNTDOWN; countdownTicks = 0;
                            task = Bukkit.getScheduler().runTaskTimer(plugin,() -> guardedTick(token),0,1);
                        }));
                } catch (Exception e) { fail(e.toString()); }
            }));
        } catch (Exception e) { fail(e.toString()); }
    }
    private void guardedTick(long token) {
        if (generation != token || match == null) { stopTask(); return; }
        try {
            if (phase == Phase.COUNTDOWN) {
                if (countdownTicks < 100) {
                    if (countdownTicks % 20 == 0) announce("countdown",Map.of("seconds",String.valueOf(5-countdownTicks/20)));
                    countdownTicks++;
                } else if (!startRequested) {
                    startRequested = true;
                    api.transitionMatch(match.id(),MatchState.RUNNING,"ArenaTNTRun",null).orTimeout(15,TimeUnit.SECONDS)
                        .whenComplete((m,e) -> onMain(() -> { if (token == generation && match != null && e != null) fail("Start: " + root(e)); }));
                }
            } else if (phase == Phase.RUNNING) tick();
        } catch (Exception e) { fail(e.toString()); }
    }
    @Override public void start() {
        mainThread();
        if (match == null) return;
        if (phase != Phase.COUNTDOWN || countdownTicks < 100 || !startRequested) { fail("Start before five-second countdown"); return; }
        phase = Phase.RUNNING; announce("start",Map.of());
    }
    private void tick() throws Exception {
        round.advance();
        // Batch eliminations before checking victory: simultaneous falls have stable ordering.
        for (UUID id : participants) if (round.alive(id)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || p.isDead() || !inside(p.getLocation()) || p.getLocation().getY() < arena.eliminationY()) {
                eliminate(id); continue;
            }
            if (p.getGameMode() != GameMode.ADVENTURE || p.isFlying() || p.isGliding()) { eliminate(id); continue; }
            round.layer(id,arena.layerAt(p.getLocation().getY()));
            // Sample all blocks under the player's 0.6-block footprint, including edge cases.
            Location l = p.getLocation(); int y = (int)Math.floor(l.getY() - .05);
            for (int x = (int)Math.floor(l.getX()-.29); x <= (int)Math.floor(l.getX()+.29); x++)
                for (int z = (int)Math.floor(l.getZ()-.29); z <= (int)Math.floor(l.getZ()+.29); z++) {
                    if (!arena.floor(x,y,z) || p.getWorld().getBlockAt(x,y,z).isEmpty()) continue;
                    for (TntArena.Layer layer : arena.layers()) if (layer.contains(x,y,z))
                        for (int by=layer.topY()-layer.depth()+1; by<=layer.topY(); by++) removals.schedule(new BlockJournal.Cell(x,by,z),round.tick(),arena.removalDelayTicks());
                }
        }
        for (BlockJournal.Cell c : removals.due(round.tick())) {
            World w = Objects.requireNonNull(Bukkit.getWorld(arena.world()));
            // Never prime TNT, never call breakNaturally, never create explosions or drops.
            w.getBlockAt(c.x(),c.y(),c.z()).setType(Material.AIR,false);
        }
        if (round.complete(arena.timeoutSeconds()*20L)) { complete(); return; }
        if (round.tick() % 10 == 0) {
            allPlayers(p -> {
                if (viewers.contains(p.getUniqueId()) || !round.alive(p.getUniqueId())) containSpectator(p);
                updateBoard(p);
            });
        }
    }
    private void eliminate(UUID id) {
        if (!round.eliminate(id)) return;
        Player p = Bukkit.getPlayer(id);
        announce("eliminated",Map.of("player",p == null ? id.toString() : p.getName(),"alive",String.valueOf(round.aliveCount())));
        if (p != null) { p.setGameMode(GameMode.SPECTATOR); teleport(p,arena.spectator()); }
    }
    public void quit(Player player) {
        if (participants.contains(player.getUniqueId())) {
            if (phase == Phase.RUNNING) eliminate(player.getUniqueId());
            else if (phase != Phase.ENDING) fail("Participant disconnected during preparation/countdown");
        }
        viewers.remove(player.getUniqueId());
        // Restore before Bukkit saves quit state; if it fails, the file is retained for join recovery.
        recover(player);
    }
    private void complete() throws Exception {
        phase = Phase.ENDING; stopTask();
        List<ResultEntry> entries = round.standings().stream().map(s -> new ResultEntry(s.player(),teams.get(s.player()),s.position(),0,s.winner())).toList();
        result = new MatchResult(match.id()+"-tntrun-v1",match.id(),entries,false,Instant.now());
        outbox.save(result); // Persist delivery intent BEFORE cleanup or calling Core.
        String winner = entries.stream().filter(ResultEntry::winner).map(e -> {
            Player p = Bukkit.getPlayer(e.playerId()); return p == null ? e.playerId().toString() : p.getName();
        }).findFirst().orElse(plugin.getConfig().getString("messages.no-winner","Nenhum"));
        announce("finished",Map.of("winner",winner));
        cleanup();
        deliverResult();
    }
    @Override public void finish(MatchResult official) {
        mainThread();
        if (match != null && !match.id().equals(official.matchId())) { plugin.getLogger().warning("Ignoring finish for another match"); return; }
        result=official;
        // Core also calls reset. Result ack happens only after durable Core confirmation.
        cleanup();
        if (!outbox.pending()) {
            try { Files.deleteIfExists(session); } catch (Exception e) { plugin.getLogger().severe("Session ack retained: "+e); }
        }
    }
    @Override public void cancel(String reason) {
        mainThread(); String id=match == null ? null : match.id();
        announce("cancelled",Map.of("reason",String.valueOf(reason))); cleanup();
        if (id != null && !outbox.pending()) cancelCore(id,reason);
    }
    @Override public void reset() { mainThread(); cancel("Adapter reset"); }
    @Override public Optional<MatchResult> getResult() { return Optional.ofNullable(result); }
    @Override public Set<UUID> getParticipants() { return Set.copyOf(participants); }
    public boolean active(UUID id) { return participants.contains(id) || viewers.contains(id); }
    public boolean protectedPlayer(UUID id) { return active(id) || (!restoring && snapshots.pending(id)); }
    public boolean frozen(UUID id) { return participants.contains(id) && phase != Phase.RUNNING && phase != Phase.ENDING; }
    public boolean spectator(UUID id) { return viewers.contains(id) || (participants.contains(id) && round != null && !round.alive(id)); }
    public boolean inside(Location location) {
        return arena != null && location.getWorld().getName().equals(arena.world()) && arena.bounds().contains(location.getX(),location.getY(),location.getZ());
    }
    public boolean protectedLocation(Location l) {
        return arenas.all().stream().anyMatch(a -> a.world().equals(l.getWorld().getName()) && a.bounds().contains(l.getX(),l.getY(),l.getZ()));
    }
    public boolean allowTeleport(UUID id, Location to) {
        if (restoring || internalMove) return true;
        if (active(id)) return false;
        return match == null || !inside(to);
    }
    public boolean restoring() { return restoring; }
    public boolean internalMove() { return internalMove; }
    public void containSpectator(Player p) {
        if (!inside(p.getLocation())) teleport(p,arena.spectator());
        if (p.getSpectatorTarget() != null) p.setSpectatorTarget(null);
    }
    public void watch(Player p) throws Exception {
        if (match == null || phase == Phase.PREPARING || phase == Phase.ENDING || active(p.getUniqueId()) || snapshots.pending(p.getUniqueId()))
            throw new IllegalStateException("Spectating unavailable");
        snapshots.capture(p); viewers.add(p.getUniqueId());
        try { normalize(p); p.setGameMode(GameMode.SPECTATOR); teleport(p,arena.spectator()); updateBoard(p); }
        catch (Exception e) { viewers.remove(p.getUniqueId()); recover(p); throw e; }
    }
    public void leave(Player p) {
        if (participants.contains(p.getUniqueId())) {
            if (phase == Phase.RUNNING) eliminate(p.getUniqueId());
            else if (phase != Phase.ENDING) fail("Participant left before start");
            return; // Eliminated participants remain spectators until round cleanup.
        }
        viewers.remove(p.getUniqueId()); recover(p);
    }
    public boolean recover(Player p) {
        restoring=true;
        try {
            boolean restored = snapshots.restore(p);
            if (!restored) plugin.getLogger().warning("Original world unavailable for " + p.getUniqueId());
            return restored;
        } catch (Exception e) { plugin.getLogger().severe("Player recovery retained: " + p.getUniqueId()+": "+e); return false; }
        finally { restoring=false; }
    }
    public void recoverJoin(Player p) {
        if (participants.contains(p.getUniqueId()) && phase == Phase.RUNNING) {
            // Disconnect is elimination even when Core permits reconnection.
            if (round.alive(p.getUniqueId())) eliminate(p.getUniqueId());
            try {
                if (!recover(p)) throw new IllegalStateException("Player recovery pending");
                snapshots.capture(p); normalize(p); p.setGameMode(GameMode.SPECTATOR); teleport(p,arena.spectator());
            } catch (Exception e) { plugin.getLogger().severe("Spectator reconnect failed: "+e); p.kick(Component.text("Recuperação pendente.")); }
            return;
        }
        if (!recover(p)) p.kick(Component.text(plugin.getConfig().getString("messages.recovery-failed","Recuperação pendente; contate um administrador.")));
    }
    public void requestCancel(String reason) {
        if (match != null) fail(reason);
    }
    private void fail(String reason) {
        plugin.getLogger().severe("TNT Run cancelled: " + reason);
        cancel(reason);
    }
    private void cancelCore(String id,String reason) {
        interruptedMatch=id;
        api.match(id).thenCompose(m -> {
            if (m.state() == MatchState.FINISHED || m.state() == MatchState.CANCELLED) return CompletableFuture.completedFuture(m);
            return api.transitionMatch(id,MatchState.CANCELLED,"ArenaTNTRun",reason);
        }).orTimeout(15,TimeUnit.SECONDS).whenComplete((m,e) -> onMain(() -> {
            if (e == null && Objects.equals(interruptedMatch,id)) {
                try { Files.deleteIfExists(session); interruptedMatch=null; } catch (Exception failure) { plugin.getLogger().severe("Session recovery retained: "+failure); }
            }
            else if (e != null) plugin.getLogger().severe("Core cancellation pending for "+id+": "+root(e));
        }));
    }
    private void cleanup() {
        generation++; stopTask(); restoring=true;
        try {
            Set<UUID> ids = new HashSet<>(participants); ids.addAll(viewers);
            for (UUID id : ids) { Player p=Bukkit.getPlayer(id); if (p != null && !recover(p)) p.kick(Component.text("TNT Run: recuperação pendente.")); }
            try { if (!blocks.restore()) plugin.getLogger().severe("Block recovery pending: world unavailable"); }
            catch (Exception e) { plugin.getLogger().severe("Block recovery retained: "+e); }
        } finally {
            restoring=false; match=null; arena=null; phase=null; round=null; participants.clear(); viewers.clear(); teams.clear(); removals.clear(); startRequested=false;
        }
    }
    /** Startup and administrator recovery. Does not resume an interrupted game. */
    public void recoverPersistent() throws Exception {
        mainThread(); if (match != null) throw new IllegalStateException("Cancel active match first");
        String abandoned = Files.exists(session) ? Files.readString(session) : blocks.matchId();
        if (!blocks.restore()) plugin.getLogger().warning("World unavailable; block journal retained");
        for (Player p : Bukkit.getOnlinePlayers()) recoverJoin(p);
        if (outbox.pending()) deliverResult();
        else if (abandoned != null) cancelCore(abandoned,"Interrupted TNT Run recovered");
        else if (interruptedMatch != null) cancelCore(interruptedMatch,"Retry recovery cancellation");
    }
    public void deliverResult() {
        mainThread(); if (delivering || !outbox.pending()) return;
        final MatchResult saved;
        try { saved=outbox.read(); } catch (Exception e) { plugin.getLogger().severe("Result outbox retained: "+e); return; }
        delivering=true;
        api.match(saved.matchId()).thenCompose(m -> {
            if (m.state() == MatchState.FINISHED && saved.id().equals(m.resultId())) return CompletableFuture.completedFuture(true);
            if (m.state() != MatchState.RUNNING) return CompletableFuture.failedFuture(new IllegalStateException("Core state/result conflict: "+m.state()));
            return api.submitResult(saved.matchId(),saved,"ArenaTNTRun");
        }).orTimeout(15,TimeUnit.SECONDS).whenComplete((ok,e) -> onMain(() -> {
            delivering=false;
            if (e != null || !Boolean.TRUE.equals(ok)) { plugin.getLogger().severe("Result delivery pending; use /tntrun recover: "+(e == null ? "Core rejected result" : root(e))); return; }
            try { Files.deleteIfExists(session); outbox.acknowledge(); } catch (Exception failure) { plugin.getLogger().severe("Outbox ack retained: "+failure); }
        }));
    }
    public void shutdown() {
        mainThread();
        String id = match == null ? null : match.id();
        cleanup();
        if (id != null && !outbox.pending()) {
            // Fire Core cancellation without scheduling callbacks into a disabled plugin.
            api.match(id).thenCompose(m -> m.state() == MatchState.FINISHED || m.state() == MatchState.CANCELLED ? CompletableFuture.completedFuture(m)
                : api.transitionMatch(id,MatchState.CANCELLED,"ArenaTNTRun","Plugin disabled"))
                .exceptionally(e -> { plugin.getLogger().warning("Core shutdown cancellation: "+root(e)); return null; });
        }
    }
    private void normalize(Player p) {
        p.closeInventory(); p.getInventory().clear(); p.setGameMode(GameMode.ADVENTURE); p.setAllowFlight(false); p.setFlying(false);
        p.setCollidable(false); p.setInvulnerable(true); p.setGravity(true); p.setWalkSpeed(.2f); p.setFlySpeed(.1f);
        p.setHealth(Math.min(20,p.getMaxHealth())); p.setFoodLevel(20); p.setSaturation(20); p.setExhaustion(0);
        p.setFireTicks(0); p.setFreezeTicks(0); p.setFallDistance(0); p.setVelocity(new org.bukkit.util.Vector());
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        updateBoard(p);
    }
    private void updateBoard(Player p) {
        if (arena == null) return;
        Scoreboard b = p.getScoreboard(); Objective o = b.getObjective("tntrun");
        if (o == null) { o=b.registerNewObjective("tntrun",Criteria.DUMMY,Component.text(plugin.getConfig().getString("scoreboard.title","TNT RUN"))); o.setDisplaySlot(DisplaySlot.SIDEBAR); }
        for (String entry : new HashSet<>(b.getEntries())) b.resetScores(entry);
        Map<String,String> vars = Map.of("arena",arena.id(),"alive",String.valueOf(round == null ? participants.size() : round.aliveCount()),
            "seconds",String.valueOf(Math.max(0,arena.timeoutSeconds()-(round == null ? 0 : round.tick()/20))),
            "state",String.valueOf(phase),"removed",String.valueOf(removals.removedCount()));
        List<String> lines=plugin.getConfig().getStringList("scoreboard.lines"); int score=lines.size();
        for (String line : lines) o.getScore(expand(line,vars)+ChatColor.values()[score % 16]).setScore(score--);
    }
    private void teleport(Player p,TntArena.Point point) {
        internalMove=true;
        try {
            if (!p.teleport(new Location(Objects.requireNonNull(Bukkit.getWorld(arena.world())),point.x(),point.y(),point.z(),point.yaw(),point.pitch())))
                throw new IllegalStateException("Teleport refused: "+p.getName());
        } finally { internalMove=false; }
    }
    private void announce(String key,Map<String,String> vars) {
        String text=expand(plugin.getConfig().getString("messages."+key,key),vars); allPlayers(p -> p.sendMessage(Component.text(text)));
    }
    private static String expand(String text,Map<String,String> vars) { for (var e : vars.entrySet()) text=text.replace("{"+e.getKey()+"}",e.getValue()); return text; }
    private void allPlayers(Consumer<Player> action) { Set<UUID> ids=new HashSet<>(participants); ids.addAll(viewers); ids.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(action); }
    private void stopTask() { if (task != null) { task.cancel(); task=null; } }
    private void onMain(Runnable action) { if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin,action); }
    private static void mainThread() { if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("TNT Run operation off main thread"); }
    private static String root(Throwable e) { while (e.getCause() != null) e=e.getCause(); return e.toString(); }
}

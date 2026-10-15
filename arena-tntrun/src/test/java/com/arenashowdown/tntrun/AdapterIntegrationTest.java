package com.arenashowdown.tntrun;

import com.arenashowdown.api.OrchestrationApi;
import com.arenashowdown.api.minigame.MinigameContext;
import com.arenashowdown.api.model.*;
import com.arenashowdown.tntrun.arena.*;
import com.arenashowdown.tntrun.persistence.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

class AdapterIntegrationTest {
    @TempDir Path temp;
    FakePaper paper;
    FakePaper.Human a,b;
    ArenaStore arenas;
    TntRunAdapter game;
    BlockJournal blocks;
    PlayerSnapshots snapshots;
    ResultOutbox outbox;
    OrchestrationApi api;
    MatchState state;
    MatchResult submitted;
    boolean rejectResult;
    CompletableFuture<List<ParticipantInfo>> teamLoad;
    List<MatchState> transitions;
    Map<String,Object> original;
    Location originalLocation;
    Map<String,String> originalFloors;

    @BeforeEach void setup() throws Exception {
        paper=new FakePaper(); a=paper.human(1); b=paper.human(2); original=new HashMap<>(a.state); originalLocation=a.location.clone();
        arenas=new ArenaStore(temp.resolve("arenas")); arenas.save(ArenaAndPersistenceTest.example()); paper.floors(arenas.get("teste").orElseThrow()); originalFloors=new HashMap<>(paper.cells);
        state=MatchState.PREPARING; transitions=new ArrayList<>();
        teamLoad=CompletableFuture.completedFuture(List.of(new ParticipantInfo(a.id,"a","red"),new ParticipantInfo(b.id,"b",null)));
        api=FakePaper.proxy(OrchestrationApi.class,(method,args) -> switch(method) {
            case "participants" -> teamLoad;
            case "match" -> CompletableFuture.completedFuture(match());
            case "transitionMatch" -> {
                MatchState next=(MatchState)args[1];
                if (!state.canTransitionTo(next)) yield CompletableFuture.failedFuture(new IllegalStateException(state+" -> "+next));
                state=next; transitions.add(next);
                if (next==MatchState.RUNNING) paper.callbacks.add(() -> game.start());
                if (next==MatchState.CANCELLED) paper.callbacks.add(() -> { game.cancel(String.valueOf(args[3])); game.reset(); });
                yield CompletableFuture.completedFuture(match());
            }
            case "submitResult" -> {
                if (rejectResult) yield CompletableFuture.failedFuture(new IllegalStateException("DB unavailable"));
                submitted=(MatchResult)args[1]; state=MatchState.FINISHED; paper.callbacks.add(() -> { game.finish(submitted); game.reset(); });
                yield CompletableFuture.completedFuture(true);
            }
            default -> null;
        });
        game=newAdapter();
    }
    private ArenaMatch match() {
        return new ArenaMatch("m","champ",1,"tntrun","teste",state,List.of(a.id,b.id),List.of(),true,Instant.now(),null,null,submitted==null ? null : submitted.id());
    }
    private TntRunAdapter newAdapter() throws Exception {
        Path recovery=temp.resolve("recovery"); blocks=new BlockJournal(recovery); snapshots=new PlayerSnapshots(recovery.resolve("players")); outbox=new ResultOutbox(recovery);
        return new TntRunAdapter(paper.plugin(temp),api,arenas,blocks,snapshots,outbox);
    }
    private void prepare() { game.prepare(new MinigameContext(match())); paper.flush(); }
    private void running() { prepare(); paper.ticks(101); assertEquals(MatchState.RUNNING,state); }
    private void assertRestored() {
        for (String key : original.keySet()) assertEquals(original.get(key),a.state.get(key),key);
        assertEquals(originalLocation,a.location); assertEquals(originalFloors,paper.cells); assertFalse(blocks.pending()); assertFalse(snapshots.pending(a.id)); assertTrue(a.saved>0);
    }
    @Test void snapshotsAreDurableBeforeCountdownAndStartWaitsFiveSeconds() {
        prepare(); assertTrue(blocks.pending()); assertTrue(snapshots.pending(a.id)); assertEquals(GameMode.ADVENTURE,a.player.getGameMode());
        paper.ticks(100); assertEquals(MatchState.COUNTDOWN,state); assertTrue(game.frozen(a.id)); assertEquals(originalFloors,paper.cells);
        paper.ticks(1); assertEquals(MatchState.RUNNING,state); assertFalse(game.frozen(a.id));
        assertEquals(List.of(MatchState.COUNTDOWN,MatchState.RUNNING),transitions);
    }
    @Test void stationaryPlayersRemoveSandAndSupportAfterExactDelayWithoutPhysics() {
        running(); paper.ticks(8); assertEquals("minecraft:sand",paper.at(3,75,3)); assertEquals("minecraft:tnt",paper.at(3,74,3));
        paper.ticks(1); assertEquals("minecraft:air",paper.at(3,75,3)); assertEquals("minecraft:air",paper.at(3,74,3));
        game.requestCancel("test"); paper.flush(); assertRestored();
    }
    @Test void fallProducesCompleteRankedCoreResultWithTeamAndRestoresEverything() {
        running(); paper.ticks(9); a.location.setY(51); paper.ticks(1);
        assertNotNull(submitted); assertEquals("m-tntrun-v1",submitted.id()); assertEquals(2,submitted.entries().size());
        assertEquals(b.id,submitted.entries().getFirst().playerId()); assertTrue(submitted.entries().getFirst().winner());
        assertEquals("red",submitted.entries().getLast().teamId()); assertFalse(submitted.tie());
        assertRestored(); assertFalse(outbox.pending()); assertFalse(game.busy());
    }
    @Test void simultaneousFallHasNoWinnerAndDeterministicUuidRanks() {
        running(); a.location.setY(51); b.location.setY(51); paper.ticks(1);
        assertEquals(List.of(a.id,b.id),submitted.entries().stream().map(ResultEntry::playerId).toList()); assertTrue(submitted.entries().stream().noneMatch(ResultEntry::winner)); assertRestored();
    }
    @Test void cancellationDuringPreparationInvalidatesLateTeamCallback() {
        teamLoad=new CompletableFuture<>(); game.prepare(new MinigameContext(match())); game.requestCancel("admin"); paper.flush();
        teamLoad.complete(List.of()); paper.flush(); assertEquals(MatchState.CANCELLED,state); assertEquals(originalLocation,a.location);
        assertEquals(originalFloors,paper.cells); assertFalse(blocks.pending()); assertFalse(game.busy());
    }
    @Test void earlyCoreStartCancelsAndRestoresInsteadOfSkippingCountdown() {
        prepare(); state=MatchState.RUNNING; game.start(); paper.flush(); assertEquals(MatchState.CANCELLED,state); assertRestored();
    }
    @Test void interruptedRoundRecoversFromNewStoreInstancesAndCancelsCore() throws Exception {
        running(); paper.ticks(9); assertNotEquals(originalFloors,paper.cells);
        paper.timers.forEach(t -> t.cancelled=true); // Simulated process interruption; no adapter shutdown/cleanup.
        game=newAdapter(); game.recoverPersistent(); paper.flush(); assertRestored(); assertEquals(MatchState.CANCELLED,state); assertFalse(game.busy());
    }
    @Test void missingWorldRetainsJournalAndBlocksNewRoundsUntilRecovery() throws Exception {
        running(); paper.ticks(9); paper.timers.forEach(t -> t.cancelled=true); paper.worldLoaded=false;
        game=newAdapter(); game.recoverPersistent(); paper.flush(); assertTrue(blocks.pending()); assertTrue(game.busy()); assertFalse(game.supportsArena("teste"));
        paper.worldLoaded=true; a.online=true; b.online=true; game.recoverPersistent(); paper.flush(); assertRestored(); assertFalse(game.busy());
    }
    @Test void resultFailureRestoresPlayersAndReplaysStableOutboxOnRestart() throws Exception {
        running(); rejectResult=true; a.location.setY(51); paper.ticks(1); assertRestored(); assertTrue(outbox.pending()); assertTrue(game.busy());
        MatchResult saved=outbox.read(); rejectResult=false; game=newAdapter(); game.recoverPersistent(); paper.flush();
        assertEquals(saved,submitted); assertFalse(outbox.pending()); assertFalse(game.busy());
    }
    @Test void coreAlreadyFinishedAcknowledgesOutboxWithoutResubmitting() throws Exception {
        running(); rejectResult=true; a.location.setY(51); paper.ticks(1); MatchResult saved=outbox.read(); submitted=saved; state=MatchState.FINISHED;
        game=newAdapter(); game.recoverPersistent(); paper.flush(); assertFalse(outbox.pending()); assertEquals(saved,submitted);
    }
    @Test void spectatorGetsOwnSnapshotAndLeaveRestoresOriginalState() throws Exception {
        running(); var viewer=paper.human(3); Location origin=viewer.location.clone(); var board=viewer.player.getScoreboard();
        game.watch(viewer.player); assertEquals(GameMode.SPECTATOR,viewer.player.getGameMode()); assertTrue(snapshots.pending(viewer.id));
        game.leave(viewer.player); assertEquals(origin,viewer.location); assertSame(board,viewer.player.getScoreboard()); assertFalse(snapshots.pending(viewer.id));
    }
    @Test void disconnectEliminatesEvenWhenCoreAllowsReconnectAndRestoresBeforeQuitSave() {
        running(); game.quit(a.player); a.online=false; assertEquals(originalLocation,a.location); assertFalse(snapshots.pending(a.id));
        paper.ticks(1); assertEquals(b.id,submitted.entries().getFirst().playerId()); assertRestored();
    }
    @Test void protectionsCancelDamageBreakingAndExternalTeleport() {
        running(); TntRunListener listener=new TntRunListener(null,game);
        var source=FakePaper.proxy(org.bukkit.damage.DamageSource.class,(m,args) -> null);
        EntityDamageEvent damage=new EntityDamageEvent(a.player,EntityDamageEvent.DamageCause.FALL,source,4); listener.damage(damage); assertTrue(damage.isCancelled());
        BlockBreakEvent broken=new BlockBreakEvent(paper.world.getBlockAt(3,75,3),a.player); listener.breakBlock(broken); assertTrue(broken.isCancelled());
        PlayerTeleportEvent tp=new PlayerTeleportEvent(a.player,a.location,new Location(paper.world,-10,90,-10)); listener.teleport(tp); assertTrue(tp.isCancelled());
    }
    @Test void malformedSnapshotIsRetainedAndNeverPartiallyApplied() throws Exception {
        prepare(); Path file=temp.resolve("recovery/players/"+a.id+".yml"); Files.writeString(file,"schema: 1\nuuid: "+a.id+"\n");
        Location before=a.location.clone(); assertFalse(game.recover(a.player)); assertEquals(before,a.location); assertTrue(Files.exists(file));
    }
    @Test void refusesToOverwriteActiveMatch() {
        prepare(); assertThrows(IllegalStateException.class,() -> game.prepare(new MinigameContext(match()))); assertTrue(game.frozen(a.id)); assertTrue(blocks.pending());
    }
    @Test void lowerLayersAreRemovedByFootstepsAndAllLayersRestore() {
        running(); a.location=new Location(paper.world,3.5,66,3.5); paper.ticks(9);
        assertEquals("minecraft:air",paper.at(3,65,3)); assertEquals("minecraft:air",paper.at(3,64,3));
        game.requestCancel("layers"); paper.flush(); assertRestored();
    }
    @Test void timeoutProducesDeterministicCompleteResult() throws Exception {
        var yaml=ArenaAndPersistenceTest.example(); yaml.set("timeout-seconds",5); arenas.save(yaml);
        running(); a.location.setY(66); paper.ticks(99); assertNull(submitted); paper.ticks(1);
        assertEquals(b.id,submitted.entries().getFirst().playerId()); assertTrue(submitted.entries().getFirst().winner()); assertEquals(2,submitted.entries().size()); assertRestored();
    }
    @Test void reconnectRecapturesOriginalBeforeBecomingSpectator() {
        running(); game.quit(a.player); a.online=false; a.online=true; game.recoverJoin(a.player);
        assertEquals(GameMode.SPECTATOR,a.player.getGameMode()); assertTrue(snapshots.pending(a.id));
        paper.ticks(1); assertRestored();
    }
    @Test void successfulReloadChangesOnlyNextMatchesIncludingMessagesTimeoutAndRemovalDelay() throws Exception {
        game.configuration().initialize();
        var messages=ArenaStore.read(temp.resolve("messages.yml")); messages.set("start","OLD START"); messages.set("countdown","OLD {seconds}");
        Files.writeString(temp.resolve("messages.yml"),messages.saveToString()); game.configuration().reload(); prepare();
        var arena=ArenaStore.read(temp.resolve("arenas/teste.yml")); arena.set("timeout-seconds",5); arena.set("removal-delay-ticks",1);
        Files.writeString(temp.resolve("arenas/teste.yml"),arena.saveToString()); messages.set("start","NEW START"); messages.set("countdown","NEW {seconds}");
        Files.writeString(temp.resolve("messages.yml"),messages.saveToString());
        // This is an actual administrative reload while COUNTDOWN is active.
        a.state.put("Permission",true);
        new TntRunCommand(paper.plugin(temp),arenas,game).onCommand(a.player,null,"tntrun",new String[]{"reload"});
        paper.ticks(100); assertEquals(MatchState.COUNTDOWN,state); assertTrue(a.messages.contains(Component.text("OLD 5"))); assertFalse(a.messages.contains(Component.text("NEW 5")));
        paper.ticks(1); assertTrue(a.messages.contains(Component.text("OLD START"))); paper.ticks(8); assertEquals("minecraft:sand",paper.at(3,75,3));
        paper.ticks(1); assertEquals("minecraft:air",paper.at(3,75,3)); paper.ticks(91); assertEquals(MatchState.RUNNING,state);
        a.location.setY(51); paper.ticks(1); assertEquals(MatchState.FINISHED,state); assertFalse(game.busy());
        state=MatchState.PREPARING; submitted=null; prepare(); paper.ticks(101);
        assertTrue(a.messages.contains(Component.text("NEW START"))); paper.ticks(1); assertEquals("minecraft:sand",paper.at(3,75,3));
        paper.ticks(1); assertEquals("minecraft:air",paper.at(3,75,3)); paper.ticks(98); assertEquals(MatchState.FINISHED,state);
    }
    @Test void activeArenaKeepsProtectionEvenWhenReloadMovesItsDefinition() throws Exception {
        game.configuration().initialize(); running(); var yaml=ArenaStore.read(temp.resolve("arenas/teste.yml"));
        yaml.set("bounds.min-x",100); yaml.set("bounds.max-x",115);
        for (String key : yaml.getConfigurationSection("layers").getKeys(false)) { yaml.set("layers."+key+".min-x",101); yaml.set("layers."+key+".max-x",114); }
        yaml.set("spawns.0.x",103.5); yaml.set("spawns.1.x",111.5); yaml.set("spectator.x",108);
        Files.writeString(temp.resolve("arenas/teste.yml"),yaml.saveToString()); game.configuration().reload();
        assertTrue(game.protectedLocation(new Location(paper.world,3,75,3))); assertFalse(game.protectedLocation(new Location(paper.world,103,75,3)));
        assertTrue(game.inside(a.location)); paper.ticks(9); assertEquals("minecraft:air",paper.at(3,75,3));
        game.requestCancel("test"); paper.flush(); assertRestored();
        assertFalse(game.protectedLocation(new Location(paper.world,3,75,3))); assertTrue(game.protectedLocation(new Location(paper.world,103,75,3)));
    }
    @Test void reloadWhileWaitingForTeamCallbackCannotReplacePreparationSnapshot() throws Exception {
        game.configuration().initialize(); teamLoad=new CompletableFuture<>(); game.prepare(new MinigameContext(match()));
        var yaml=ArenaStore.read(temp.resolve("arenas/teste.yml")); yaml.set("spawns.0.x",4.5); yaml.set("removal-delay-ticks",1);
        Files.writeString(temp.resolve("arenas/teste.yml"),yaml.saveToString()); game.configuration().reload();
        teamLoad.complete(List.of()); paper.flush(); assertEquals(3.5,a.location.getX());
        paper.ticks(101); paper.ticks(8); assertEquals("minecraft:sand",paper.at(3,75,3));
        game.requestCancel("test"); paper.flush(); assertRestored();
    }
    @Test void abilitySettingsHaveNoEffectsAndChampionshipChangesWaitUntilNextMatch() throws Exception {
        game.configuration().initialize(); game.configuration().saveChampionship("champ","dash",2,3,70); running();
        assertEquals(2,game.configuredAbilities().orElseThrow().settings().get("dash").uses());
        game.configuration().saveChampionship("champ","dash",4,7,90);
        assertEquals(2,game.configuredAbilities().orElseThrow().settings().get("dash").uses());
        assertEquals(GameMode.ADVENTURE,a.player.getGameMode()); assertFalse(a.player.getAllowFlight());
        assertEquals(.2f,a.player.getWalkSpeed()); assertTrue(game.configuredAbilities().orElseThrow().settings().values().stream().noneMatch(v -> v.enabled()));
        game.requestCancel("test"); paper.flush(); assertRestored();
    }
}

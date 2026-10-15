package com.arenashowdown.tntrun;

import com.arenashowdown.tntrun.arena.ArenaStore;
import com.arenashowdown.tntrun.persistence.PlayerSnapshots;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotValidationTest {
    @TempDir Path temp;

    @Test void invalidExperienceProgressRetainsSnapshotWithoutChangingPlayer() throws Exception {
        rejectsWithoutMutation("exp",2);
    }
    @Test void invalidWalkSpeedRetainsSnapshotWithoutChangingPlayer() throws Exception {
        rejectsWithoutMutation("walk-speed",2);
    }
    @Test void fractionalLevelRetainsSnapshotWithoutChangingPlayer() throws Exception {
        rejectsWithoutMutation("level",1.5);
    }
    @Test void negativeLevelRetainsSnapshotWithoutChangingPlayer() throws Exception {
        rejectsWithoutMutation("level",-1);
    }
    @Test void invalidIntegerTypesOverflowAndFloatOverflowAreRejectedBeforeMutation() throws Exception {
        rejectsWithoutMutation("total-exp",-1);
        rejectsWithoutMutation("total-exp",2147483648L);
        rejectsWithoutMutation("freeze",-1);
        rejectsWithoutMutation("held-slot",1.5);
        rejectsWithoutMutation("fire",2147483648L);
        rejectsWithoutMutation("food",1.5);
        rejectsWithoutMutation("saturation",Double.MAX_VALUE);
        rejectsWithoutMutation("yaw",Double.MAX_VALUE);
        rejectsWithoutMutation("health",Double.MIN_VALUE);
        rejectsWithoutMutation("exp","0.5");
        rejectsWithoutMutation("velocity",List.of(0,"0",0));
    }
    @Test void legalSpeedBoundariesNegativeFireAndUnboundedScalarStatesArePreserved() throws Exception {
        FakePaper paper=new FakePaper(); var human=paper.human(1);
        human.state.put("WalkSpeed",-1f); human.state.put("FlySpeed",1f); human.state.put("FireTicks",-20);
        human.state.put("FoodLevel",25); human.state.put("Saturation",25f); human.state.put("Exhaustion",-1f); human.state.put("FallDistance",-2f);
        human.state.put("Exp",1f); human.state.put("Level",0); human.state.put("TotalExperience",0); human.state.put("FreezeTicks",0);
        Map<String,Object> original=new HashMap<>(human.state); Location location=human.location.clone();
        PlayerSnapshots snapshots=new PlayerSnapshots(temp); snapshots.capture(human.player);
        human.player.setGameMode(GameMode.ADVENTURE); human.location=new Location(paper.world,10,76,10);
        assertTrue(snapshots.restore(human.player));
        original.forEach((key,value) -> assertEquals(value,human.state.get(key),key));
        assertEquals(location,human.location); assertFalse(snapshots.pending(human.id)); assertEquals(1,human.saved);
    }

    private void rejectsWithoutMutation(String key,Object invalid) throws Exception {
        FakePaper paper=new FakePaper(); var human=paper.human(1);
        Path directory=temp.resolve(UUID.randomUUID().toString()); PlayerSnapshots snapshots=new PlayerSnapshots(directory);
        snapshots.capture(human.player);
        Path file=directory.resolve(human.id+".yml"); var yaml=ArenaStore.read(file); yaml.set(key,invalid); Files.writeString(file,yaml.saveToString());
        byte[] savedSnapshot=Files.readAllBytes(file);
        // Distinct current state proves rejection occurs before applying the captured original.
        human.location=new Location(paper.world,10,76,10); human.player.setGameMode(GameMode.ADVENTURE); human.player.setHealth(3);
        Map<String,Object> before=new HashMap<>(human.state); Location location=human.location.clone(); var items=human.items.clone();
        assertThrows(IllegalStateException.class,() -> snapshots.restore(human.player),key);
        assertEquals(location,human.location,key); assertEquals(before,human.state,key); assertArrayEquals(items,human.items,key);
        assertEquals(0,human.teleports,key); assertEquals(0,human.saved,key); assertTrue(snapshots.pending(human.id),key);
        assertArrayEquals(savedSnapshot,Files.readAllBytes(file),key);
    }
}

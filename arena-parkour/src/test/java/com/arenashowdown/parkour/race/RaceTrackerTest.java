package com.arenashowdown.parkour.race;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RaceTrackerTest {
    private final UUID alice=UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID bob=UUID.fromString("00000000-0000-0000-0000-000000000002");
    @Test void requiresSequentialCheckpointsBeforeFinish(){RaceTracker race=new RaceTracker(List.of(alice),2,10);race.start(1_000);assertFalse(race.checkpoint(alice,2));assertFalse(race.finish(alice,2_000));assertTrue(race.checkpoint(alice,1));assertTrue(race.checkpoint(alice,2));assertTrue(race.finish(alice,2_000));assertEquals(1,race.standings().getFirst().position());}
    @Test void ordersByTime(){RaceTracker race=new RaceTracker(List.of(alice,bob),0,0);race.start(100);assertTrue(race.finish(bob,200));assertTrue(race.finish(alice,300));assertEquals(List.of(bob,alice),race.standings().stream().map(RaceTracker.Standing::playerId).toList());}
    @Test void treatsCloseTimesAsTie(){RaceTracker race=new RaceTracker(List.of(alice,bob),0,10);race.start(0);race.finish(alice,1_000_000);race.finish(bob,9_000_000);assertEquals(1,race.standings().get(0).position());assertEquals(1,race.standings().get(1).position());}
    @Test void unfinishedPlayersAreExcludedAtTimeout(){RaceTracker race=new RaceTracker(List.of(alice,bob),0,0);race.start(0);race.finish(alice,1_000);assertEquals(List.of(alice),race.standings().stream().map(RaceTracker.Standing::playerId).toList());assertFalse(race.allFinished());}
}

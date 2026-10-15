package com.arenashowdown.tntrun;

import com.arenashowdown.tntrun.game.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SurvivalRoundTest {
    private final UUID a=new UUID(0,1), b=new UUID(0,2), c=new UUID(0,3), d=new UUID(0,4);
    @Test void laterEliminationRanksAheadAndAllEntriesArePresent() {
        SurvivalRound r=new SurvivalRound(List.of(a,b,c,d)); r.advance(); r.eliminate(b); r.advance(); r.eliminate(c); r.advance(); r.eliminate(a);
        assertTrue(r.complete(100)); assertEquals(List.of(d,a,c,b),r.standings().stream().map(SurvivalRound.Standing::player).toList());
        assertEquals(List.of(1,2,3,4),r.standings().stream().map(SurvivalRound.Standing::position).toList());
        assertEquals(1,r.standings().stream().filter(SurvivalRound.Standing::winner).count());
    }
    @Test void simultaneousFallsIndependentOfInsertionAndEliminationOrder() {
        SurvivalRound left=new SurvivalRound(List.of(c,a,b)), right=new SurvivalRound(List.of(b,c,a));
        left.advance(); right.advance(); left.eliminate(c); left.eliminate(a); left.eliminate(b); right.eliminate(b); right.eliminate(c); right.eliminate(a);
        assertEquals(left.standings(),right.standings()); assertEquals(List.of(a,b,c),left.standings().stream().map(SurvivalRound.Standing::player).toList());
        assertTrue(left.standings().stream().noneMatch(SurvivalRound.Standing::winner));
    }
    @Test void timeoutPrefersHighestLayerThenUuidAndIsExact() {
        SurvivalRound r=new SurvivalRound(List.of(c,b,a)); r.layer(a,2); r.layer(b,1); r.layer(c,1);
        for (int i=0;i<99;i++) r.advance(); assertFalse(r.complete(100)); r.advance(); assertTrue(r.complete(100));
        assertEquals(List.of(b,c,a),r.standings().stream().map(SurvivalRound.Standing::player).toList()); assertTrue(r.standings().getFirst().winner());
    }
    @Test void repeatedEliminationAndOutsidersDoNotChangeOrder() {
        SurvivalRound r=new SurvivalRound(List.of(a,b)); assertTrue(r.eliminate(a)); r.advance(); assertFalse(r.eliminate(a)); assertFalse(r.eliminate(c));
        assertEquals(1,r.aliveCount()); assertFalse(r.alive(c)); assertTrue(r.alive(b));
    }
    @Test void refusesDuplicateOrSingleParticipants() {
        assertThrows(IllegalArgumentException.class,() -> new SurvivalRound(List.of(a,a))); assertThrows(IllegalArgumentException.class,() -> new SurvivalRound(List.of(a)));
    }
    @Test void delayedRemovalIsOnceOnlyAndDoesNotPostponeOnRepeatedFootsteps() {
        RemovalQueue<String> q=new RemovalQueue<>(); q.schedule("sand",10,8); q.schedule("sand",17,8); q.schedule("tnt",10,8);
        assertTrue(q.due(17).isEmpty()); assertEquals(List.of("sand","tnt"),q.due(18)); assertTrue(q.due(100).isEmpty());
        q.schedule("sand",100,8); assertTrue(q.due(108).isEmpty()); assertEquals(2,q.removedCount());
        q.clear(); q.schedule("sand",0,1); assertEquals(List.of("sand"),q.due(1));
    }
    @Test void removalDelayMustBePositive() { assertThrows(IllegalArgumentException.class,() -> new RemovalQueue<>().schedule("x",1,0)); }
}

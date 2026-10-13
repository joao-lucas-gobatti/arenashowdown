package com.arenashowdown.api;

import com.arenashowdown.api.minigame.MinigameAdapter;
import com.arenashowdown.api.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Public orchestration contract for minigame plugins. Persistence remains owned by ArenaCore. */
public interface OrchestrationApi {
    void registerMinigame(MinigameAdapter adapter);
    void unregisterMinigame(String id);
    CompletableFuture<ArenaMatch> createMatch(String id,String championshipId,int round,String minigame,String arena,List<UUID> participants,boolean reconnectAllowed,Instant scheduledAt,String actor);
    CompletableFuture<ArenaMatch> transitionMatch(String id,MatchState next,String actor,String reason);
    CompletableFuture<Boolean> submitResult(String matchId,MatchResult result,String actor);
    CompletableFuture<ArenaMatch> match(String id);
    CompletableFuture<List<ArenaMatch>> calendar(String championshipId);
}

package com.arenashowdown.core.service;

import com.arenashowdown.api.OrchestrationApi;
import com.arenashowdown.api.minigame.MinigameAdapter;
import com.arenashowdown.api.model.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class ArenaOrchestrationService implements OrchestrationApi {
    private final MatchManager matches;
    public ArenaOrchestrationService(MatchManager matches){this.matches=matches;}
    @Override public void registerMinigame(MinigameAdapter adapter){matches.registerMinigame(adapter);}
    @Override public void unregisterMinigame(String id){matches.unregisterMinigame(id);}
    @Override public CompletableFuture<ArenaMatch> createMatch(String id,String championshipId,int round,String minigame,String arena,List<UUID> participants,boolean reconnectAllowed,Instant scheduledAt,String actor){return matches.create(id,championshipId,round,minigame,arena,participants,reconnectAllowed,scheduledAt,actor);}
    @Override public CompletableFuture<ArenaMatch> transitionMatch(String id,MatchState next,String actor,String reason){return matches.transition(id,next,actor,reason);}
    @Override public CompletableFuture<Boolean> submitResult(String matchId,MatchResult result,String actor){return matches.finish(matchId,result,actor);}
    @Override public CompletableFuture<ArenaMatch> match(String id){return matches.match(id);}
    @Override public CompletableFuture<List<ArenaMatch>> calendar(String championshipId){return matches.calendar(championshipId);}
    @Override public CompletableFuture<ChampionshipDetails> championship(String id){return matches.championship(id);}
    @Override public CompletableFuture<List<ParticipantInfo>> participants(String id){return matches.participants(id);}
}

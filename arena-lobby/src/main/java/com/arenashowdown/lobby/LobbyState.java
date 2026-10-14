package com.arenashowdown.lobby;

import com.arenashowdown.api.*;
import com.arenashowdown.api.model.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

final class LobbyState {
    record View(String name,String state,int round,String next,List<RankingEntry> ranking,List<ParticipantInfo> participants){
        static View empty(){return new View("Nenhum campeonato","INDISPONIVEL",0,"-",List.of(),List.of());}
    }
    private final ArenaApi arena; private final OrchestrationApi orchestration; volatile View view=View.empty();
    LobbyState(ArenaApi arena,OrchestrationApi orchestration){this.arena=arena;this.orchestration=orchestration;}
    CompletableFuture<View> refresh(String id,int limit){
        var championship=orchestration.championship(id);var ranking=arena.ranking(id,limit);var participants=orchestration.participants(id);
        return CompletableFuture.allOf(championship,ranking,participants).thenApply(x->{var c=championship.join();String next=c.currentRound()<c.minigames().size()?c.minigames().get(c.currentRound()):"-";return view=new View(c.name(),c.state().name(),c.currentRound(),next,ranking.join(),participants.join());});
    }
}

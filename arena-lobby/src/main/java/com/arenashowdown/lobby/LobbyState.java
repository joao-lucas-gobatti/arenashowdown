package com.arenashowdown.lobby;
import com.arenashowdown.api.*;import com.arenashowdown.api.model.*;import java.util.*;import java.util.concurrent.CompletableFuture;
final class LobbyState {
 record View(String name,String state,int round,int totalRounds,String next,List<RankingEntry> ranking,List<ParticipantInfo> participants,List<ArenaMatch> matches){
  static View empty(){return new View("Nenhum campeonato","INDISPONIVEL",0,0,"-",List.of(),List.of(),List.of());}
  boolean active(UUID id){return matches.stream().anyMatch(m->m.participants().contains(id)&&active(m.state()));}
  boolean minigameActive(String id){return matches.stream().anyMatch(m->m.minigame().equalsIgnoreCase(id)&&active(m.state()));}
  String team(UUID id){return participants.stream().filter(p->p.playerId().equals(id)).map(ParticipantInfo::teamId).filter(Objects::nonNull).findFirst().orElse("");}
  int points(UUID id){return ranking.stream().filter(r->r.playerId().equals(id)).mapToInt(RankingEntry::points).findFirst().orElse(0);}
  int position(UUID id){for(int i=0;i<ranking.size();i++)if(ranking.get(i).playerId().equals(id))return i+1;return 0;}
  private static boolean active(MatchState s){return s==MatchState.PREPARING||s==MatchState.COUNTDOWN||s==MatchState.RUNNING;}
 }
 private final ArenaApi arena;private final OrchestrationApi orchestration;volatile View view=View.empty();
 LobbyState(ArenaApi arena,OrchestrationApi orchestration){this.arena=arena;this.orchestration=orchestration;}
 CompletableFuture<View> refresh(String id,int limit){var c=orchestration.championship(id);var r=arena.ranking(id,limit);var p=orchestration.participants(id);var m=orchestration.calendar(id);return CompletableFuture.allOf(c,r,p,m).thenApply(x->{var d=c.join();String next=d.currentRound()<d.minigames().size()?d.minigames().get(d.currentRound()):"-";return view=new View(d.name(),d.state().name(),d.currentRound(),d.minigames().size(),next,r.join(),p.join(),m.join());});}
}

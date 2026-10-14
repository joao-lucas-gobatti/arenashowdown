package com.arenashowdown.lobby;
import com.arenashowdown.api.model.*;import org.junit.jupiter.api.Test;import java.time.*;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class LobbyStateTest{
 @Test void detectsActiveParticipantAndKeepsRealRanking(){UUID id=UUID.randomUUID();ArenaMatch match=new ArenaMatch("m","c",1,"parkour","a",MatchState.RUNNING,List.of(id),List.of(),false,Instant.now(),Instant.now(),Duration.ZERO,null);LobbyState.View view=new LobbyState.View("Copa","RUNNING",1,8,"parkour",List.of(new RankingEntry(id,"Jogador",10)),List.of(new ParticipantInfo(id,"Jogador","ouro")),List.of(match));assertTrue(view.active(id));assertTrue(view.minigameActive("parkour"));assertEquals(10,view.points(id));assertEquals("ouro",view.team(id));}
 @Test void finishedMatchDoesNotBlockLobby(){UUID id=UUID.randomUUID();ArenaMatch match=new ArenaMatch("m","c",1,"parkour","a",MatchState.FINISHED,List.of(id),List.of(),false,Instant.now(),Instant.now(),Duration.ZERO,"r");LobbyState.View view=new LobbyState.View("Copa","RUNNING",1,8,"skywars",List.of(),List.of(),List.of(match));assertFalse(view.active(id));}
}

package com.arenashowdown.core.service;

import com.arenashowdown.api.minigame.*;
import com.arenashowdown.api.model.*;
import com.arenashowdown.core.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class OrchestrationIntegrationTest {
    @TempDir Path directory;
    private Path file; private Database database; private ExecutorService executor; private ArenaService arena; private ChampionshipManager championships; private MatchManager matches; private JdbcArenaRepository base; private JdbcOrchestrationRepository orchestration;
    private final UUID alice=UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID bob=UUID.fromString("00000000-0000-0000-0000-000000000002");
    @BeforeEach void setup()throws Exception{file=directory.resolve("arena.db");database=Database.sqlite(file);executor=Executors.newFixedThreadPool(2);base=new JdbcArenaRepository(database);orchestration=new JdbcOrchestrationRepository(database);arena=new ArenaService(base,executor);championships=new ChampionshipManager(base,orchestration,executor);matches=new MatchManager(orchestration,executor);}
    @AfterEach void close(){arena.close();database.close();}

    @Test void completeTeamChampionshipWithSimulatedMinigame() {
        var adapter=new FakeMinigame("parkour");matches.registerMinigame(adapter);
        championships.create("duplas","Copa de Duplas",ChampionshipMode.TEAMS,List.of("parkour"),TieBreaker.MOST_WINS,"test").join();
        arena.registerPlayer("duplas",alice,"Alice",null).join();arena.registerPlayer("duplas",bob,"Bob",null).join();
        championships.createTeam("duplas","red","Red","test").join();championships.createTeam("duplas","blue","Blue","test").join();
        championships.assignTeam("duplas",alice,"red","test").join();championships.assignTeam("duplas",bob,"blue","test").join();
        championships.transition("duplas",ChampionshipState.PREPARING,"test").join();championships.transition("duplas",ChampionshipState.RUNNING,"test").join();
        matches.create("duplas-r1","duplas",1,"parkour","arena-a",List.of(alice,bob),true,Instant.now(),"test").join();
        matches.transition("duplas-r1",MatchState.PREPARING,"test",null).join();matches.transition("duplas-r1",MatchState.COUNTDOWN,"test",null).join();matches.transition("duplas-r1",MatchState.RUNNING,"test",null).join();
        assertTrue(adapter.prepared);assertTrue(adapter.started);
        var result=new MatchResult("result-r1","duplas-r1",List.of(new ResultEntry(alice,"red",1,10,true),new ResultEntry(bob,"blue",2,7,false)),false,Instant.now());
        assertTrue(matches.finish("duplas-r1",result,"test").join());
        assertTrue(adapter.finished);assertTrue(adapter.reset);
        assertThrows(CompletionException.class,()->matches.finish("duplas-r1",result,"test").join());
        assertEquals(10,arena.ranking("duplas",10).join().getFirst().points());
        assertEquals("red",championships.teamRanking("duplas",10).join().getFirst().teamId());
        assertEquals(1,championships.details("duplas").join().currentRound());
        assertEquals("red",championships.finish("duplas","test").join());
    }

    @Test void validatesStatesTieRestartAndConnectionLifecycle() {
        championships.create("solo","Solo",ChampionshipMode.INDIVIDUAL,List.of("skywars"),TieBreaker.NAME,"test").join();
        arena.registerPlayer("solo",alice,"Alice",null).join();arena.registerPlayer("solo",bob,"Bob",null).join();
        assertThrows(CompletionException.class,()->matches.create("early","solo",1,"skywars","a",List.of(alice),true,Instant.now(),"test").join());
        championships.transition("solo",ChampionshipState.PREPARING,"test").join();championships.transition("solo",ChampionshipState.RUNNING,"test").join();
        championships.transition("solo",ChampionshipState.PAUSED,"test").join();championships.transition("solo",ChampionshipState.RUNNING,"test").join();
        matches.create("solo-r1","solo",1,"skywars","a",List.of(alice,bob),true,Instant.now(),"test").join();
        matches.transition("solo-r1",MatchState.PREPARING,"test",null).join();matches.transition("solo-r1",MatchState.COUNTDOWN,"test",null).join();matches.transition("solo-r1",MatchState.RUNNING,"test",null).join();
        matches.disconnected("solo-r1",alice).join();assertEquals(ParticipantStatus.DISCONNECTED,matches.participantStatus("solo-r1",alice).join().orElseThrow());assertTrue(matches.reconnected("solo-r1",alice).join());
        var tie=new MatchResult("tie","solo-r1",List.of(new ResultEntry(alice,null,1,5,true),new ResultEntry(bob,null,1,5,true)),true,Instant.now());assertTrue(matches.finish("solo-r1",tie,"test").join());
        assertEquals("Alice",arena.ranking("solo",10).join().getFirst().playerName());
        assertEquals(MatchState.WAITING,matches.restartRound("solo-r1","solo-r1-retry","test").join().state());
    }

    @Test void survivesRepositoryRestart() throws Exception {
        championships.create("persist","Persist",ChampionshipMode.INDIVIDUAL,List.of("tnt-run"),TieBreaker.MOST_WINS,"test").join();arena.registerPlayer("persist",alice,"Alice",null).join();
        arena.close();database.close();
        try(Database reopened=Database.sqlite(file)){var details=new JdbcOrchestrationRepository(reopened).championshipDetails("persist");assertTrue(details.isPresent());assertEquals(List.of("tnt-run"),details.orElseThrow().minigames());assertEquals("Alice",new JdbcArenaRepository(reopened).ranking("persist",10).getFirst().playerName());}
        executor=Executors.newSingleThreadExecutor();database=Database.sqlite(file);arena=new ArenaService(new JdbcArenaRepository(database),executor);
    }

    private static final class FakeMinigame implements MinigameAdapter {private final String id;boolean prepared,started,finished,reset;MatchResult result;FakeMinigame(String id){this.id=id;}public String id(){return id;}public void prepare(MinigameContext c){prepared=true;}public void start(){started=true;}public void finish(MatchResult r){finished=true;result=r;}public void cancel(String reason){}public void reset(){reset=true;}public Optional<MatchResult>getResult(){return Optional.ofNullable(result);}public Set<UUID>getParticipants(){return Set.of();}}
}

package com.arenashowdown.tntrun;

import com.arenashowdown.api.OrchestrationApi;
import com.arenashowdown.api.model.*;
import com.arenashowdown.core.persistence.*;
import com.arenashowdown.core.service.*;
import com.arenashowdown.tntrun.arena.*;
import com.arenashowdown.tntrun.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual Core orchestration + fresh SQLite; only the Bukkit boundary is simulated. */
class CoreBridgeTest {
    @TempDir Path temp;
    @Test void completeTntRunPersistsScoredTeamResultAndReplayNeverDuplicatesPoints() throws Exception {
        FakePaper paper=new FakePaper(); var a=paper.human(1); var b=paper.human(2);
        ArenaStore arenas=new ArenaStore(temp.resolve("arenas")); arenas.save(ArenaAndPersistenceTest.example()); paper.floors(arenas.get("teste").orElseThrow());
        Path db=temp.resolve("fresh-core.db"), recovery=temp.resolve("recovery");
        try (Database database=Database.sqlite(db)) {
            JdbcArenaRepository base=new JdbcArenaRepository(database); JdbcOrchestrationRepository repo=new JdbcOrchestrationRepository(database);
            ChampionshipManager champs=new ChampionshipManager(base,repo,Runnable::run);
            champs.create("cup","TNT Cup",ChampionshipMode.TEAMS,List.of("tntrun"),TieBreaker.MOST_WINS,"test").join();
            base.registerPlayer("cup",a.id,"Alice",null); base.registerPlayer("cup",b.id,"Bob",null);
            champs.createTeam("cup","red","Red","test").join(); champs.createTeam("cup","blue","Blue","test").join();
            champs.assignTeam("cup",a.id,"red","test").join(); champs.assignTeam("cup",b.id,"blue","test").join();
            champs.transition("cup",ChampionshipState.PREPARING,"test").join(); champs.transition("cup",ChampionshipState.RUNNING,"test").join();
            Path scoring=temp.resolve("scoring.yml"); Files.writeString(scoring,"scoring:\n  positions: {first: 10, second: 7, third: 5}\n");
            MatchManager matches=new MatchManager(repo,Runnable::run,paper.callbacks::add,new YamlScorePolicy(scoring.toFile()));
            OrchestrationApi api=new ArenaOrchestrationService(matches);
            TntRunAdapter game=new TntRunAdapter(paper.plugin(temp),api,arenas,new BlockJournal(recovery),new PlayerSnapshots(recovery.resolve("players")),new ResultOutbox(recovery)); api.registerMinigame(game);
            assertTrue(game.supportsArena("teste"));
            api.createMatch("cup-r1","cup",1,"tntrun","teste",List.of(a.id,b.id),true,Instant.now(),"test").join();
            api.transitionMatch("cup-r1",MatchState.PREPARING,"test",null).join(); paper.flush(); paper.ticks(101);
            assertEquals(MatchState.RUNNING,api.match("cup-r1").join().state()); a.location.setY(51); paper.ticks(1);
            assertEquals(MatchState.FINISHED,api.match("cup-r1").join().state()); assertEquals(1,champs.details("cup").join().currentRound());
            assertEquals(b.id,base.ranking("cup",10).getFirst().playerId()); assertEquals(10,base.ranking("cup",10).getFirst().points());
            assertEquals("blue",champs.teamRanking("cup",10).join().getFirst().teamId());
            assertEquals(10,champs.teamRanking("cup",10).join().getFirst().points()); assertFalse(game.busy());
            ResultOutbox replay=new ResultOutbox(recovery); replay.save(game.getResult().orElseThrow());
            game.recoverPersistent(); paper.flush(); assertFalse(replay.pending());
            assertEquals(10,base.ranking("cup",10).getFirst().points());
            try (var connection=database.connection(); var statement=connection.createStatement(); var rows=statement.executeQuery("SELECT COUNT(*) FROM score_events")) {
                assertTrue(rows.next()); assertEquals(2,rows.getInt(1));
            }
        }
        try (Database reopened=Database.sqlite(db)) {
            assertEquals(MatchState.FINISHED,new JdbcOrchestrationRepository(reopened).match("cup-r1").orElseThrow().state());
            assertEquals(10,new JdbcArenaRepository(reopened).ranking("cup",10).getFirst().points());
        }
    }
}

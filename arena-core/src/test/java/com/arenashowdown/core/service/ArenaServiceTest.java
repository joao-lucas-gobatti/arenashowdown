package com.arenashowdown.core.service;

import com.arenashowdown.api.model.ChampionshipState;
import com.arenashowdown.core.persistence.*;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

class ArenaServiceTest {
    private Path dbFile; private Database database; private ArenaService service;
    @BeforeEach void setup() throws Exception { dbFile=Files.createTempFile("arena-core-test-",".db"); database=Database.sqlite(dbFile); service=new ArenaService(new JdbcArenaRepository(database),Executors.newSingleThreadExecutor()); }
    @AfterEach void cleanup() throws Exception { service.close(); database.close(); Files.deleteIfExists(dbFile); }

    @Test void lifecycleRejectsInvalidTransitions() {
        var created=service.createChampionship("copa_1","Copa 1").join();
        assertEquals(ChampionshipState.WAITING,created.state());
        assertThrows(Exception.class,() -> service.transition("copa_1",ChampionshipState.RUNNING).join());
        assertEquals(ChampionshipState.PREPARING,service.transition("copa_1",ChampionshipState.PREPARING).join().state());
        assertEquals(ChampionshipState.RUNNING,service.transition("copa_1",ChampionshipState.RUNNING).join().state());
    }

    @Test void duplicateEventDoesNotDuplicatePoints() {
        UUID player=UUID.randomUUID();
        service.createChampionship("final","Final").join();
        service.registerPlayer("final",player,"Steve",null).join();
        service.transition("final",ChampionshipState.PREPARING).join();
        service.transition("final",ChampionshipState.RUNNING).join();
        assertTrue(service.awardPoints("final",player,10,"parkour","round-1-finish").join());
        assertFalse(service.awardPoints("final",player,10,"parkour","round-1-finish").join());
        assertEquals(10,service.ranking("final",10).join().getFirst().points());
    }

    @Test void registrationClosesAfterWaiting() {
        service.createChampionship("closed","Closed").join();
        service.transition("closed",ChampionshipState.PREPARING).join();
        assertThrows(Exception.class,() -> service.registerPlayer("closed",UUID.randomUUID(),"Alex",null).join());
    }
}

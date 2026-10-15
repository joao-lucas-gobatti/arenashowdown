package com.arenashowdown.tntrun;

import com.arenashowdown.api.OrchestrationApi;
import com.arenashowdown.tntrun.arena.ArenaStore;
import com.arenashowdown.tntrun.persistence.*;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class CommandPermissionsTest {
    @TempDir Path temp;
    @Test void unauthorizedCommandsCannotWriteArenaDefinitionsOrCreateSpectatorSnapshots() throws Exception {
        FakePaper paper=new FakePaper(); var p=paper.human(1); ArenaStore store=new ArenaStore(temp.resolve("arenas")); store.reload();
        TntRunCommand cmd=command(paper,store);
        cmd.onCommand(p.player,null,"tntrun",new String[]{"create","arena"});
        cmd.onCommand(p.player,null,"tntrun",new String[]{"spectate"});
        cmd.onCommand(p.player,null,"tntrun",new String[]{"championship","cup","dash","2","1.5","60"});
        assertFalse(Files.exists(store.directory().resolve("arena.draft"))); assertFalse(Files.exists(temp.resolve("recovery/players")));
        assertFalse(Files.exists(temp.resolve("championships/cup.yml")));
        assertEquals(java.util.List.of("help"),cmd.onTabComplete(p.player,null,"tntrun",new String[]{""}));
    }
    @Test void authorizedDraftCreationNeverOverwritesExistingDraftAndRejectsTraversal() throws Exception {
        FakePaper paper=new FakePaper(); var p=paper.human(1); p.state.put("Permission",true);
        ArenaStore store=new ArenaStore(temp.resolve("arenas")); store.reload(); TntRunCommand cmd=command(paper,store);
        cmd.onCommand(p.player,null,"tntrun",new String[]{"create","arena"}); Path file=store.directory().resolve("arena.draft"); byte[] before=Files.readAllBytes(file);
        p.location=new Location(paper.world,99,90,99); cmd.onCommand(p.player,null,"tntrun",new String[]{"create","arena"});
        assertArrayEquals(before,Files.readAllBytes(file)); assertTrue(store.all().isEmpty());
        cmd.onCommand(p.player,null,"tntrun",new String[]{"create","../escape"}); assertFalse(Files.exists(temp.resolve("escape.draft")));
    }
    @Test void invalidSaveLeavesDraftForRepairAndDoesNotActivateArena() throws Exception {
        FakePaper paper=new FakePaper(); var p=paper.human(1); p.state.put("Permission",true);
        ArenaStore store=new ArenaStore(temp.resolve("arenas")); store.reload(); TntRunCommand cmd=command(paper,store);
        cmd.onCommand(p.player,null,"tntrun",new String[]{"create","arena"}); cmd.onCommand(p.player,null,"tntrun",new String[]{"save","arena"});
        assertTrue(Files.exists(store.directory().resolve("arena.draft"))); assertFalse(Files.exists(store.directory().resolve("arena.yml"))); assertTrue(store.all().isEmpty());
    }
    @Test void administratorCanStoreFutureParametersThroughCommandsWithoutJavaChanges() throws Exception {
        FakePaper paper=new FakePaper(); var p=paper.human(1); p.state.put("Permission",true);
        ArenaStore store=new ArenaStore(temp.resolve("arenas")); store.save(ArenaAndPersistenceTest.example()); TntRunCommand cmd=command(paper,store);
        cmd.onCommand(p.player,null,"tntrun",new String[]{"ability","teste","dash","5","2.0","60"});
        cmd.onCommand(p.player,null,"tntrun",new String[]{"save","teste"});
        assertEquals(5,store.get("teste").orElseThrow().abilities().settings().get("dash").uses());
        assertFalse(store.get("teste").orElseThrow().abilities().settings().get("dash").enabled());
        cmd.onCommand(p.player,null,"tntrun",new String[]{"championship","cup","double-jump","3","1.2","80"});
        var yaml=com.arenashowdown.tntrun.arena.ArenaStore.read(temp.resolve("championships/cup.yml"));
        assertEquals(3,yaml.getInt("abilities.double-jump.uses")); assertFalse(yaml.getBoolean("abilities.double-jump.enabled"));
    }
    private TntRunCommand command(FakePaper paper,ArenaStore arenas) throws Exception {
        var plugin=paper.plugin(temp); var recovery=temp.resolve("recovery");
        var api=FakePaper.proxy(OrchestrationApi.class,(m,a) -> null);
        var game=new TntRunAdapter(plugin,api,arenas,new BlockJournal(recovery),new PlayerSnapshots(recovery.resolve("players")),new ResultOutbox(recovery));
        // Command implementation uses JavaPlugin only for config; Plugin is sufficient for testing.
        return new TntRunCommand(plugin,arenas,game);
    }
}

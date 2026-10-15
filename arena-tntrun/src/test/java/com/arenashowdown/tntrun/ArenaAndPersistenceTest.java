package com.arenashowdown.tntrun;

import com.arenashowdown.api.model.*;
import com.arenashowdown.tntrun.arena.*;
import com.arenashowdown.tntrun.persistence.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ArenaAndPersistenceTest {
    @TempDir Path temp;
    static YamlConfiguration example() throws Exception {
        YamlConfiguration y=new YamlConfiguration();
        try (var in=ArenaAndPersistenceTest.class.getResourceAsStream("/arena.example.yml")) { y.loadFromString(new String(Objects.requireNonNull(in).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)); }
        return y;
    }
    @Test void exampleLoadsThreeLayersAndBoundariesAreHalfOpen() throws Exception {
        TntArena a=ArenaStore.parse(example()); assertEquals(3,a.layers().size()); assertTrue(a.floor(1,74,1)); assertFalse(a.floor(1,73,1));
        assertTrue(a.bounds().contains(15.99,84,15.99)); assertFalse(a.bounds().contains(16,84,1));
        assertEquals(0,a.layerAt(76)); assertEquals(1,a.layerAt(70)); assertEquals(2,a.layerAt(60));
    }
    @Test void rejectsMissingFractionalNonFiniteAndUnsafeConfiguration() throws Exception {
        for (String key : List.of("bounds.min-x","timeout-seconds","spectator.x","elimination-y")) {
            YamlConfiguration y=example(); y.set(key,null); assertThrows(IllegalArgumentException.class,() -> ArenaStore.parse(y));
        }
        YamlConfiguration fractional=example(); fractional.set("bounds.min-x",.1); assertThrows(IllegalArgumentException.class,() -> ArenaStore.parse(fractional));
        YamlConfiguration infinite=example(); infinite.set("elimination-y",Double.NaN); assertThrows(IllegalArgumentException.class,() -> ArenaStore.parse(infinite));
        YamlConfiguration materials=example(); materials.set("materials",List.of("CHEST")); assertThrows(IllegalArgumentException.class,() -> ArenaStore.parse(materials));
        YamlConfiguration traversal=example(); traversal.set("id","../world"); assertThrows(IllegalArgumentException.class,() -> ArenaStore.parse(traversal));
    }
    @Test void refusesOverlappingLayersMissingSpawnsAndOversizeFloors() throws Exception {
        YamlConfiguration y=example(); y.set("layers.1.top-y",75); YamlConfiguration overlap=y; assertThrows(IllegalArgumentException.class,() -> ArenaStore.parse(overlap));
        y=example(); y.set("spawns.1",null); YamlConfiguration one=y; assertThrows(IllegalArgumentException.class,() -> ArenaStore.parse(one));
        y=example(); y.set("spawns.0.y",70); YamlConfiguration unsafe=y; assertThrows(IllegalArgumentException.class,() -> ArenaStore.parse(unsafe));
        assertThrows(IllegalArgumentException.class,() -> new TntArena("big","test",new TntArena.Bounds(0,0,0,1000,100,1000),List.of(new TntArena.Layer(0,1000,0,1000,75,1)),List.of(new TntArena.Point(1,76,1,0,0),new TntArena.Point(2,76,2,0,0)),new TntArena.Point(1,90,1,0,0),20,60,8,Set.of("TNT")));
    }
    @Test void yamlReloadIsTransactionalAndDraftsAreIgnored() throws Exception {
        ArenaStore store=new ArenaStore(temp); store.save(example()); assertEquals(1,store.all().size());
        Files.writeString(temp.resolve("unfinished.draft"),"not a valid arena"); store.reload(); assertTrue(store.get("teste").isPresent());
        Files.writeString(temp.resolve("bad.yml"),"bad: ["); assertThrows(Exception.class,store::reload); assertTrue(store.get("teste").isPresent());
    }
    @Test void refusesOverlappingArenasWithoutWritingNewFile() throws Exception {
        ArenaStore store=new ArenaStore(temp); store.save(example()); YamlConfiguration second=example(); second.set("id","second");
        assertThrows(IllegalArgumentException.class,() -> store.save(second)); assertFalse(Files.exists(temp.resolve("second.yml")));
    }
    @Test void atomicFilesNeverOverwriteOriginalAndLeaveNoTemporaryDebris() throws Exception {
        Path p=temp.resolve("snap.yml"); AtomicFiles.write(p,"original",false);
        assertThrows(FileAlreadyExistsException.class,() -> AtomicFiles.write(p,"replacement",false)); assertEquals("original",Files.readString(p));
        AtomicFiles.write(p,"updated",true); assertEquals("updated",Files.readString(p));
        try (var files=Files.list(temp)) { assertEquals(1,files.count()); }
    }
    @Test void resultOutboxSurvivesNewInstanceAndKeepsStableIdAndTeams() throws Exception {
        MatchResult result=new MatchResult("m-tntrun-v1","m",List.of(new ResultEntry(new UUID(0,1),"red",1,0,true),new ResultEntry(new UUID(0,2),null,2,0,false)),false,Instant.parse("2026-10-14T00:00:00Z"));
        ResultOutbox out=new ResultOutbox(temp); out.save(result); assertThrows(FileAlreadyExistsException.class,() -> out.save(result));
        ResultOutbox restarted=new ResultOutbox(temp); assertEquals(result,restarted.read()); restarted.acknowledge(); assertFalse(restarted.pending());
    }
    @Test void truncatedOutboxAndBlockJournalAreRetained() throws Exception {
        Files.writeString(temp.resolve("result.yml"),"schema: 1\nmatch: m\n"); ResultOutbox out=new ResultOutbox(temp);
        assertThrows(Exception.class,out::read); assertTrue(out.pending());
        Files.writeString(temp.resolve("blocks.yml"),"schema: 1\nmatch: m\ncount: 12\ncells: []\n"); BlockJournal blocks=new BlockJournal(temp);
        assertThrows(Exception.class,blocks::restore); assertTrue(blocks.pending());
    }
}

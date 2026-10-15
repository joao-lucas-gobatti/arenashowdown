package com.arenashowdown.tntrun;

import com.arenashowdown.tntrun.ability.AbilityConfig;
import com.arenashowdown.tntrun.arena.*;
import com.arenashowdown.tntrun.configuration.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PluginConfigurationTest {
    @TempDir Path temp;
    ArenaStore arenas;
    PluginConfiguration config;
    @BeforeEach void setup() throws Exception {
        arenas=new ArenaStore(temp.resolve("arenas")); arenas.save(ArenaAndPersistenceTest.example());
        config=new PluginConfiguration(temp,arenas); config.initialize();
    }
    private void write(String file,YamlConfiguration yaml) throws Exception { Files.writeString(temp.resolve(file),yaml.saveToString()); }
    private YamlConfiguration read(String file) throws Exception { return ArenaStore.read(temp.resolve(file)); }
    @Test void initializesEditableFilesAndCapturesImmutableMatchSettings() throws Exception {
        assertTrue(Files.exists(temp.resolve("config.yml"))); assertTrue(Files.exists(temp.resolve("messages.yml")));
        assertTrue(Files.isDirectory(temp.resolve("championships")));
        var old=config.capture("teste","cup"); var messages=read("messages.yml"); messages.set("start","New match only"); write("messages.yml",messages);
        var arena=read("arenas/teste.yml"); arena.set("timeout-seconds",5); arena.set("removal-delay-ticks",1); write("arenas/teste.yml",arena);
        config.reload(); var next=config.capture("teste","cup");
        assertEquals(180,old.arena().timeoutSeconds()); assertEquals(8,old.arena().removalDelayTicks()); assertNotEquals("New match only",old.messages().text("start"));
        assertEquals(5,next.arena().timeoutSeconds()); assertEquals(1,next.arena().removalDelayTicks()); assertEquals("New match only",next.messages().text("start"));
        assertThrows(UnsupportedOperationException.class,() -> old.messages().values().put("start","bad"));
    }
    @Test void invalidMessagesRollbackEntireReloadIncludingOtherwiseValidRulesAndScoreboard() throws Exception {
        var previous=config.current(); var previousArena=arenas.get("teste").orElseThrow();
        var arena=read("arenas/teste.yml"); arena.set("timeout-seconds",5); write("arenas/teste.yml",arena);
        var settings=read("config.yml"); settings.set("scoreboard.enabled",false); write("config.yml",settings);
        var messages=read("messages.yml"); messages.set("countdown",List.of("wrong type")); write("messages.yml",messages);
        assertThrows(IllegalArgumentException.class,config::reload); assertSame(previous,config.current()); assertSame(previousArena,arenas.get("teste").orElseThrow());
        messages.set("countdown",previous.messages().text("countdown"));
        for (Object invalidSection : List.of("not a section",List.of("not a section"),7)) {
            messages.set("commands",invalidSection); write("messages.yml",messages);
            assertThrows(IllegalArgumentException.class,config::reload); assertSame(previous,config.current()); assertSame(previousArena,arenas.get("teste").orElseThrow());
        }
    }
    @Test void malformedOrInvalidRulesDoNotPublishNewMessages() throws Exception {
        var previous=config.current(); var messages=read("messages.yml"); messages.set("start","unpublished"); write("messages.yml",messages);
        Files.writeString(temp.resolve("arenas/teste.yml"),"layers: ["); assertThrows(Exception.class,config::reload);
        assertSame(previous,config.current()); assertEquals(180,arenas.get("teste").orElseThrow().timeoutSeconds());
    }
    @Test void validationDoesNotPublishValidPendingChanges() throws Exception {
        var previous=config.current(); var messages=read("messages.yml"); messages.set("start","pending"); write("messages.yml",messages);
        config.validate(); assertSame(previous,config.current()); assertNotEquals("pending",config.current().messages().text("start"));
        config.reload(); assertEquals("pending",config.current().messages().text("start"));
    }
    @Test void invalidTechnicalSettingsAndScoreboardMessagesAreRefused() throws Exception {
        var original=read("config.yml");
        for (Object ticks : List.of(0,201,1.5,"10",Double.NaN)) {
            var yaml=read("config.yml"); yaml.set("scoreboard.update-ticks",ticks); write("config.yml",yaml);
            assertThrows(IllegalArgumentException.class,config::reload);
        }
        write("config.yml",original); var messages=read("messages.yml"); messages.set("scoreboard.lines",List.of(3)); write("messages.yml",messages);
        assertThrows(IllegalArgumentException.class,config::reload);
    }
    @Test void misspelledKeysAreRefusedInsteadOfSilentlyIgnored() throws Exception {
        var messages=read("messages.yml"); messages.set("countdonw","typo"); write("messages.yml",messages); assertThrows(IllegalArgumentException.class,config::reload);
    }
    @Test void championshipOverridesOnlySpecifiedArenaAbilityFieldsAndNeverEnableGameplay() throws Exception {
        var arena=read("arenas/teste.yml"); arena.set("abilities.dash.uses",3); arena.set("abilities.dash.strength",2.5); write("arenas/teste.yml",arena);
        Files.writeString(temp.resolve("championships/cup.yml"),"id: cup\nabilities:\n  dash: {uses: 4, cooldown-ticks: 99}\n"); config.reload();
        var match=config.capture("teste","cup"); var dash=match.abilities().settings().get("dash");
        assertEquals(4,dash.uses()); assertEquals(2.5,dash.strength()); assertEquals(99,dash.cooldownTicks()); assertFalse(dash.enabled());
        assertEquals(3,config.capture("teste","other").abilities().settings().get("dash").uses());
        assertEquals(AbilityConfig.defaults().settings().get("double-jump"),match.abilities().settings().get("double-jump"));
    }
    @Test void unavailableAbilityInChampionshipRollsBackEverything() throws Exception {
        var previous=config.current(); Files.writeString(temp.resolve("championships/cup.yml"),"id: cup\nabilities:\n  dash: {enabled: true}\n");
        assertThrows(IllegalArgumentException.class,config::reload); assertSame(previous,config.current());
    }
    @Test void championshipCommandWriterIsValidatedBeforeDiskOrPublication() throws Exception {
        config.saveChampionship("cup","dash",2,2.0,60); var captured=config.capture("teste","cup");
        byte[] before=Files.readAllBytes(temp.resolve("championships/cup.yml"));
        assertThrows(IllegalArgumentException.class,() -> config.saveChampionship("cup","dash",2,Double.NaN,60));
        assertArrayEquals(before,Files.readAllBytes(temp.resolve("championships/cup.yml")));
        config.saveChampionship("cup","dash",5,3,120); assertEquals(2,captured.abilities().settings().get("dash").uses());
        assertEquals(5,config.capture("teste","cup").abilities().settings().get("dash").uses());
    }
    @Test void migrationPreservesLegacyCustomTextWithoutRewritingExistingConfig() throws Exception {
        Path migration=temp.resolve("migration"); Files.createDirectories(migration);
        String legacy="messages:\n  start: Custom start\nscoreboard:\n  title: Old title\n  lines: ['Old line']\n";
        Files.writeString(migration.resolve("config.yml"),legacy);
        PluginConfiguration migrated=new PluginConfiguration(migration,new ArenaStore(migration.resolve("arenas"))); migrated.initialize();
        assertEquals(legacy,Files.readString(migration.resolve("config.yml"))); assertEquals("Custom start",migrated.current().messages().text("start"));
        assertEquals("Old title",migrated.current().scoreboard().title()); assertEquals(List.of("Old line"),migrated.current().scoreboard().lines());
        Files.writeString(migration.resolve("config.yml"),legacy.replace("Custom start","Ignored legacy change")); migrated.initialize();
        assertEquals("Custom start",migrated.current().messages().text("start"));
    }
}

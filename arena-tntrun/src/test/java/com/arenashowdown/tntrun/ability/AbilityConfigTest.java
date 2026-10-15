package com.arenashowdown.tntrun.ability;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AbilityConfigTest {
    private static AbilityConfig parse(String yaml) throws Exception {
        YamlConfiguration section = new YamlConfiguration();
        section.loadFromString(yaml);
        return AbilityConfig.overlay(AbilityConfig.defaults(), section);
    }

    @Test void defaultsAreDisabledAndCannotBeMutated() {
        AbilityConfig defaults = AbilityConfig.defaults();
        assertEquals(2, defaults.settings().size());
        assertTrue(defaults.settings().values().stream().noneMatch(AbilitySettings::enabled));
        assertThrows(UnsupportedOperationException.class, () -> defaults.settings().clear());
        Map<String, AbilitySettings> original = new HashMap<>(defaults.settings());
        AbilityConfig snapshot = new AbilityConfig(original);
        original.clear();
        assertEquals(defaults, snapshot);
        assertEquals(defaults, AbilityConfig.overlay(defaults, null));
    }

    @Test void overlaysInheritFieldsWithoutChangingPreviousSnapshots() throws Exception {
        AbilityConfig defaults = AbilityConfig.defaults();
        YamlConfiguration arena = new YamlConfiguration();
        arena.loadFromString("dash:\n  uses: 5\n  strength: 2.5\n");
        AbilityConfig arenaConfig = AbilityConfig.overlay(defaults, arena);
        YamlConfiguration tournament = new YamlConfiguration();
        tournament.loadFromString("dash:\n  cooldown-ticks: 120\ndouble-jump:\n  uses: 2\n");
        AbilityConfig tournamentConfig = AbilityConfig.overlay(arenaConfig, tournament);

        assertEquals(new AbilitySettings(false, 5, 2.5, 40), arenaConfig.settings().get("dash"));
        assertEquals(new AbilitySettings(false, 5, 2.5, 120), tournamentConfig.settings().get("dash"));
        assertEquals(new AbilitySettings(false, 2, 1.0, 40), tournamentConfig.settings().get("double-jump"));
        assertEquals(new AbilitySettings(false, 1, 1.5, 40), defaults.settings().get("dash"));
        arena.set("dash.uses", 99);
        tournament.set("dash.strength", 8.0);
        assertEquals(5, arenaConfig.settings().get("dash").uses());
        assertEquals(2.5, tournamentConfig.settings().get("dash").strength());
    }

    @Test void abilityParametersAreIndependent() throws Exception {
        AbilityConfig config = parse("dash:\n  uses: 3\n  strength: 2\n  cooldown-ticks: 30\ndouble-jump:\n  uses: 7\n  strength: 0.75\n  cooldown-ticks: 90\n");
        assertEquals(new AbilitySettings(false, 3, 2, 30), config.settings().get("dash"));
        assertEquals(new AbilitySettings(false, 7, 0.75, 90), config.settings().get("double-jump"));
    }

    @Test void rejectsEnablingEitherUnimplementedAbilityWithExplicitExplanation() {
        for (String id : List.of("dash", "double-jump")) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> parse(id + ":\n  enabled: true\n"));
            assertTrue(error.getMessage().contains(id));
            assertTrue(error.getMessage().contains("not implemented"));
        }
        assertThrows(IllegalArgumentException.class, () -> new AbilitySettings(true, 1, 1, 40));
    }

    @Test void rejectsUnknownAbilitiesFieldsAndIncorrectYamlShapes() {
        for (String yaml : List.of("flight: {}", "dash:\n  power: 1", "dash: true",
                "dash: [1, 2]", "dash:\n  enabled: 'false'", "dash:\n  uses: '2'",
                "dash:\n  strength: '1.5'", "dash:\n  cooldown-ticks: true")) {
            assertThrows(IllegalArgumentException.class, () -> parse(yaml), yaml);
        }
    }

    @Test void rejectsFractionalAndOverflowingIntegerFields() {
        for (String field : List.of("uses", "cooldown-ticks")) {
            for (String value : List.of("0.5", "2147483648", "-2147483649", ".inf", ".nan")) {
                String yaml = "dash:\n  " + field + ": " + value;
                assertThrows(IllegalArgumentException.class, () -> parse(yaml), yaml);
            }
        }
    }

    @Test void rejectsUnsafeParameterRangesAndNonFiniteStrength() {
        for (String setting : List.of("uses: -1", "uses: 101", "strength: 0", "strength: -1",
                "strength: 10.01", "strength: .inf", "strength: -.inf", "strength: .nan",
                "cooldown-ticks: -1", "cooldown-ticks: 72001")) {
            assertThrows(IllegalArgumentException.class, () -> parse("double-jump:\n  " + setting), setting);
        }
    }

    @Test void acceptsSupportedBoundaryValuesAndEmptyOverrides() throws Exception {
        AbilityConfig config = parse("dash:\n  enabled: false\n  uses: 0\n  strength: 0.01\n  cooldown-ticks: 0\ndouble-jump:\n  uses: 100\n  strength: 10\n  cooldown-ticks: 72000");
        assertEquals(new AbilitySettings(false, 0, 0.01, 0), config.settings().get("dash"));
        assertEquals(new AbilitySettings(false, 100, 10, 72000), config.settings().get("double-jump"));
        assertEquals(AbilityConfig.defaults(), parse("dash: {}\ndouble-jump: {}"));
        assertEquals(AbilityConfig.defaults(), parse(""));
    }

    @Test void programmaticConfigurationAlsoRejectsMissingOrUnknownIds() {
        assertThrows(IllegalArgumentException.class, () -> new AbilityConfig(Map.of()));
        Map<String, AbilitySettings> unknown = new HashMap<>(AbilityConfig.defaults().settings());
        unknown.put("flight", new AbilitySettings(false, 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new AbilityConfig(unknown));
    }
}

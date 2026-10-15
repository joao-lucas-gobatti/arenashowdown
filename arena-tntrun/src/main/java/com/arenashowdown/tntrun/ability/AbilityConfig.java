package com.arenashowdown.tntrun.ability;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Future ability settings with immutable inheritance: defaults, arena, then tournament.
 * Parsing is deliberately separate from gameplay and the shared orchestration API.
 */
public record AbilityConfig(Map<String, AbilitySettings> settings) {
    public static final String DASH = "dash";
    public static final String DOUBLE_JUMP = "double-jump";
    private static final Set<String> IDS = Set.of(DASH, DOUBLE_JUMP);
    private static final Set<String> FIELDS = Set.of("enabled", "uses", "strength", "cooldown-ticks");

    public AbilityConfig {
        Objects.requireNonNull(settings, "settings");
        if (!settings.keySet().equals(IDS)) {
            throw new IllegalArgumentException("Ability settings must contain exactly dash and double-jump");
        }
        settings = Map.copyOf(settings);
    }

    public static AbilityConfig defaults() {
        return new AbilityConfig(Map.of(
                DASH, new AbilitySettings(false, 1, 1.5, 40),
                DOUBLE_JUMP, new AbilitySettings(false, 1, 1.0, 40)));
    }

    /**
     * Applies only supplied fields and returns an independent immutable snapshot.
     * An absent section inherits every setting. Unknown IDs and fields are errors.
     */
    public static AbilityConfig overlay(AbilityConfig base, ConfigurationSection section) {
        Objects.requireNonNull(base, "base");
        if (section == null) return base;
        Map<String, AbilitySettings> result = new LinkedHashMap<>(base.settings());
        for (String id : section.getKeys(false)) {
            if (!IDS.contains(id)) throw invalid(section, id, "unknown ability ID");
            ConfigurationSection ability = section.getConfigurationSection(id);
            if (ability == null) throw invalid(section, id, "must be a YAML section");
            for (String key : ability.getKeys(false)) {
                if (!FIELDS.contains(key)) throw invalid(ability, key, "unknown ability field");
            }
            AbilitySettings previous = result.get(id);
            try {
                result.put(id, new AbilitySettings(
                        bool(ability, "enabled", previous.enabled()),
                        integer(ability, "uses", previous.uses()),
                        number(ability, "strength", previous.strength()),
                        integer(ability, "cooldown-ticks", previous.cooldownTicks())));
            } catch (IllegalArgumentException error) {
                throw invalid(section, id, error.getMessage());
            }
        }
        return new AbilityConfig(result);
    }

    private static boolean bool(ConfigurationSection section, String key, boolean fallback) {
        if (!section.contains(key)) return fallback;
        if (!(section.get(key) instanceof Boolean value)) throw invalid(section, key, "must be a boolean");
        return value;
    }

    private static int integer(ConfigurationSection section, String key, int fallback) {
        if (!section.contains(key)) return fallback;
        double value = number(section, key, fallback);
        if (value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw invalid(section, key, "must be an integer in the supported range");
        }
        return (int) value;
    }

    private static double number(ConfigurationSection section, String key, double fallback) {
        if (!section.contains(key)) return fallback;
        if (!(section.get(key) instanceof Number numeric) || !Double.isFinite(numeric.doubleValue())) {
            throw invalid(section, key, "must be a finite number");
        }
        return numeric.doubleValue();
    }

    private static IllegalArgumentException invalid(ConfigurationSection section, String key, String message) {
        String path = section.getCurrentPath();
        return new IllegalArgumentException((path == null || path.isEmpty() ? key : path + "." + key) + ": " + message);
    }
}

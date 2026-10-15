package com.arenashowdown.tntrun.ability;

/**
 * Immutable configuration reserved for a future optional ability implementation.
 * No setting in this type installs listeners or changes player behavior.
 */
public record AbilitySettings(boolean enabled, int uses, double strength, int cooldownTicks) {
    public static final int MAX_USES = 100;
    public static final double MAX_STRENGTH = 10.0;
    public static final int MAX_COOLDOWN_TICKS = 72_000;

    public AbilitySettings {
        if (enabled) {
            throw new IllegalArgumentException("Abilities are not implemented in this version; enabled must be false");
        }
        if (uses < 0 || uses > MAX_USES) {
            throw new IllegalArgumentException("Ability uses must be between 0 and " + MAX_USES);
        }
        if (!Double.isFinite(strength) || strength <= 0 || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("Ability strength must be finite, greater than 0 and at most " + MAX_STRENGTH);
        }
        if (cooldownTicks < 0 || cooldownTicks > MAX_COOLDOWN_TICKS) {
            throw new IllegalArgumentException("Ability cooldown-ticks must be between 0 and " + MAX_COOLDOWN_TICKS);
        }
    }
}

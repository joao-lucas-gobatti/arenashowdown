package com.arenashowdown.tntrun.game;

import java.util.*;

/** Tick-based survival ordering, independent of Bukkit and iteration order. */
public final class SurvivalRound {
    public record Standing(UUID player, int position, boolean winner) {}
    private final Set<UUID> players;
    private final Map<UUID, Long> eliminated = new HashMap<>();
    private final Map<UUID, Integer> layers = new HashMap<>();
    private long tick;

    public SurvivalRound(Collection<UUID> participants) {
        players = Set.copyOf(participants);
        if (players.size() < 2 || players.size() != participants.size())
            throw new IllegalArgumentException("At least two distinct participants required");
        players.forEach(id -> layers.put(id, 0));
    }

    public void advance() { tick++; }
    public long tick() { return tick; }
    public boolean alive(UUID id) { return players.contains(id) && !eliminated.containsKey(id); }
    public boolean eliminate(UUID id) {
        if (!alive(id)) return false;
        eliminated.put(id, tick);
        return true;
    }
    public void layer(UUID id, int layer) { if (alive(id)) layers.put(id, layer); }
    public int aliveCount() { return players.size() - eliminated.size(); }
    public boolean complete(long limitTicks) { return aliveCount() <= 1 || tick >= limitTicks; }

    /** Survivors first (highest layer, UUID); eliminated by last tick, then UUID. */
    public List<Standing> standings() {
        Comparator<UUID> order = Comparator.<UUID, Boolean>comparing(this::alive).reversed()
            .thenComparing((UUID id) -> alive(id) ? 0L : -eliminated.get(id))
            .thenComparingInt(id -> alive(id) ? layers.get(id) : 0)
            .thenComparing(UUID::toString);
        List<UUID> sorted = players.stream().sorted(order).toList();
        List<Standing> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++)
            result.add(new Standing(sorted.get(i), i + 1, i == 0 && alive(sorted.get(i))));
        return List.copyOf(result);
    }
}

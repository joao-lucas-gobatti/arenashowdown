package com.arenashowdown.tntrun.game;

import java.util.*;

/** Scheduling a block twice never extends its first deadline. */
public final class RemovalQueue<T> {
    private final Map<T, Long> pending = new LinkedHashMap<>();
    private final Set<T> removed = new HashSet<>();
    public void schedule(T block, long tick, long delay) {
        if (delay < 1) throw new IllegalArgumentException("delay must be positive");
        if (!removed.contains(block)) pending.putIfAbsent(block, tick + delay);
    }
    public List<T> due(long tick) {
        List<T> result = new ArrayList<>();
        pending.entrySet().removeIf(entry -> {
            if (entry.getValue() > tick) return false;
            result.add(entry.getKey()); removed.add(entry.getKey()); return true;
        });
        return List.copyOf(result);
    }
    public int removedCount() { return removed.size(); }
    public void clear() { pending.clear(); removed.clear(); }
}

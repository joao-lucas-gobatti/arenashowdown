package com.arenashowdown.api.model;

import java.time.Instant;
import java.util.List;

public record ChampionshipDetails(
    String id, String name, ChampionshipState state, ChampionshipMode mode,
    List<String> minigames, int currentRound, TieBreaker tieBreaker,
    String championId, Instant createdAt, Instant completedAt
) { public ChampionshipDetails { minigames = List.copyOf(minigames); } }

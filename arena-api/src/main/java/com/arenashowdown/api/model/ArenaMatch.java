package com.arenashowdown.api.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArenaMatch(
    String id, String championshipId, int round, String minigame, String arena,
    MatchState state, List<UUID> participants, List<String> teams,
    boolean reconnectAllowed, Instant scheduledAt, Instant startedAt,
    Duration duration, String resultId
) {
    public ArenaMatch { participants = List.copyOf(participants); teams = List.copyOf(teams); }
}

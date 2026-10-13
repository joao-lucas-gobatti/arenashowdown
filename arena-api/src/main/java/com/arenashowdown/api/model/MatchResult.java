package com.arenashowdown.api.model;

import java.time.Instant;
import java.util.List;

public record MatchResult(String id, String matchId, List<ResultEntry> entries, boolean tie, Instant recordedAt) {
    public MatchResult { entries = List.copyOf(entries); }
}

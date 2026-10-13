package com.arenashowdown.api;

import com.arenashowdown.api.model.Championship;
import com.arenashowdown.api.model.RankingEntry;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Public contract used by independently deployed minigame plugins. */
public interface ArenaApi {
    CompletableFuture<Championship> championship(String id);
    CompletableFuture<Void> registerPlayer(String championshipId, UUID playerId, String playerName, String teamId);
    CompletableFuture<Boolean> awardPoints(String championshipId, UUID playerId, int points, String source, String eventId);
    CompletableFuture<List<RankingEntry>> ranking(String championshipId, int limit);
}

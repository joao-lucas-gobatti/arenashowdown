package com.arenashowdown.api.minigame;

import com.arenashowdown.api.model.MatchResult;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Contract implemented by future minigame plugins. It never exposes persistence. */
public interface MinigameAdapter {
    String id();
    void prepare(MinigameContext context);
    void start();
    void finish(MatchResult result);
    void cancel(String reason);
    void reset();
    Optional<MatchResult> getResult();
    Set<UUID> getParticipants();
}

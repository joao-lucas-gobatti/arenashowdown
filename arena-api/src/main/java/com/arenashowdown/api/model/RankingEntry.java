package com.arenashowdown.api.model;

import java.util.UUID;

public record RankingEntry(UUID playerId, String playerName, int points) {}

package com.arenashowdown.api.model;

import java.util.UUID;

public record ResultEntry(UUID playerId, String teamId, int position, int points, boolean winner) {}

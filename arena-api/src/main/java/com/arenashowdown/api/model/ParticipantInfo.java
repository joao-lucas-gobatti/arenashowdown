package com.arenashowdown.api.model;

import java.util.UUID;

/** Public, read-only participant view exposed to orchestration clients. */
public record ParticipantInfo(UUID playerId, String playerName, String teamId) {}

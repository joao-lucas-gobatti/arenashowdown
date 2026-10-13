package com.arenashowdown.api.model;

import java.time.Instant;

public record Championship(String id, String name, ChampionshipState state, Instant createdAt) {}

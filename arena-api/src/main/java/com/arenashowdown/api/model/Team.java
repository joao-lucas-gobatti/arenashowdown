package com.arenashowdown.api.model;

import java.time.Instant;

public record Team(String championshipId, String id, String name, Instant createdAt) {}

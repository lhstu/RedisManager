package com.redismanager.domain.redis;

import java.time.Instant;
import java.util.UUID;

public record CommandHistoryEntry(
    UUID id,
    UUID profileId,
    String commandText,
    Instant executedAt,
    long durationMs,
    boolean success
) {
}

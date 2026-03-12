package com.redismanager.domain.redis;

import java.time.Instant;

public record ValueEnvelope(
    String key,
    KeyType type,
    long ttlSeconds,
    Object payload,
    Instant loadedAt,
    boolean truncated
) {
}

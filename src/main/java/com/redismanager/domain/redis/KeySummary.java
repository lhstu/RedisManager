package com.redismanager.domain.redis;

public record KeySummary(
    String key,
    KeyType type,
    long ttlSeconds,
    String encoding,
    long sizeHint
) {
}

package com.redismanager.domain.redis;

public record ValueMutation(
    String key,
    KeyType type,
    Object payload
) {
}

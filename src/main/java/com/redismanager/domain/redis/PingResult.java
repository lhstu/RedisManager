package com.redismanager.domain.redis;

public record PingResult(
    boolean success,
    String message
) {
}

package com.redismanager.domain.redis;

public record CommandResult(
    boolean success,
    String formattedOutput,
    long durationMs,
    String errorMessage
) {
}

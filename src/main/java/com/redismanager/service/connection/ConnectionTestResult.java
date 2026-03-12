package com.redismanager.service.connection;

public record ConnectionTestResult(
    boolean success,
    String message,
    long durationMs
) {
}

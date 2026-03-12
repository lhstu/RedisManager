package com.redismanager.domain.redis;

public record ScanRequest(
    String cursor,
    String matchPattern,
    int count
) {
}

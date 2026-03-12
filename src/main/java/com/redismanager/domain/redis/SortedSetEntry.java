package com.redismanager.domain.redis;

public record SortedSetEntry(
    String member,
    double score
) {
}

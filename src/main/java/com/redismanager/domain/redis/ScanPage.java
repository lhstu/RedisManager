package com.redismanager.domain.redis;

import java.util.List;

public record ScanPage<T>(
    String nextCursor,
    boolean finished,
    List<T> items
) {
}

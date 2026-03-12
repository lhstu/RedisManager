package com.redismanager.domain.connection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConnectionProfile(
    UUID id,
    String name,
    String host,
    int port,
    String username,
    String passwordRef,
    int database,
    boolean sslEnabled,
    int connectTimeoutMs,
    boolean readOnly,
    List<String> tags,
    Instant createdAt,
    Instant updatedAt
) {
}

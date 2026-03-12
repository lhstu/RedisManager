package com.redismanager.domain.connection;

import java.util.List;

public record ConnectionProfileDraft(
    String name,
    String host,
    int port,
    String username,
    String passwordRef,
    int database,
    boolean sslEnabled,
    int connectTimeoutMs,
    boolean readOnly,
    List<String> tags
) {
}

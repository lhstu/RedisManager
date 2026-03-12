package com.redismanager.domain.redis;

import java.util.List;

public record CommandRequest(
    String command,
    List<String> arguments
) {
}

package com.redismanager.service.connection;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.redis.api.RedisSession;

import java.util.concurrent.CompletionStage;

public interface ConnectionSessionService {
    CompletionStage<RedisSession> open(ConnectionProfile profile);
}

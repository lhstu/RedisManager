package com.redismanager.service.connection;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.redis.api.RedisSession;
import com.redismanager.redis.lettuce.LettuceStandaloneRedisSession;
import com.redismanager.redis.lettuce.LettuceSupport;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class DefaultConnectionSessionService implements ConnectionSessionService {
    @Override
    public CompletionStage<RedisSession> open(ConnectionProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            RedisClient client = RedisClient.create(LettuceSupport.toRedisUri(profile));
            StatefulRedisConnection<String, String> connection = client.connect();
            return new LettuceStandaloneRedisSession(profile, client, connection);
        });
    }
}

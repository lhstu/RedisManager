package com.redismanager.service.connection;

import com.redismanager.domain.connection.ConnectionProfileDraft;
import com.redismanager.redis.lettuce.LettuceSupport;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class LettuceConnectionTestService implements ConnectionTestService {
    @Override
    public CompletionStage<ConnectionTestResult> test(ConnectionProfileDraft draft) {
        return CompletableFuture.supplyAsync(() -> doTest(draft));
    }

    private ConnectionTestResult doTest(ConnectionProfileDraft draft) {
        long startedAt = System.currentTimeMillis();
        RedisClient client = RedisClient.create(LettuceSupport.toRedisUri(draft));
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            String response = connection.sync().ping();
            long durationMs = System.currentTimeMillis() - startedAt;
            return new ConnectionTestResult(true, "连接成功，服务响应: " + response, durationMs);
        } catch (RuntimeException exception) {
            long durationMs = System.currentTimeMillis() - startedAt;
            return new ConnectionTestResult(false, defaultMessage(exception), durationMs);
        } finally {
            client.shutdown();
        }
    }

    private String defaultMessage(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}

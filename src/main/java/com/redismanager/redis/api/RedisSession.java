package com.redismanager.redis.api;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.domain.redis.CommandRequest;
import com.redismanager.domain.redis.CommandResult;
import com.redismanager.domain.redis.KeySummary;
import com.redismanager.domain.redis.KeyType;
import com.redismanager.domain.redis.PingResult;
import com.redismanager.domain.redis.ScanPage;
import com.redismanager.domain.redis.ScanRequest;
import com.redismanager.domain.redis.ValueEnvelope;
import com.redismanager.domain.redis.ValueMutation;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface RedisSession extends AutoCloseable {
    ConnectionProfile profile();

    CompletionStage<PingResult> ping();

    CompletionStage<ScanPage<KeySummary>> scan(ScanRequest request);

    CompletionStage<KeyType> type(String key);

    CompletionStage<ValueEnvelope> loadValue(String key);

    CompletionStage<Void> saveValue(ValueMutation mutation);

    CompletionStage<Boolean> expire(String key, long ttlSeconds);

    CompletionStage<Boolean> persist(String key);

    CompletionStage<Long> deleteKeys(List<String> keys);

    CompletionStage<CommandResult> execute(CommandRequest request);

    @Override
    void close();
}

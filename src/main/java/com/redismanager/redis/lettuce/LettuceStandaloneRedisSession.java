package com.redismanager.redis.lettuce;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.domain.redis.CommandRequest;
import com.redismanager.domain.redis.CommandResult;
import com.redismanager.domain.redis.KeySummary;
import com.redismanager.domain.redis.KeyType;
import com.redismanager.domain.redis.PingResult;
import com.redismanager.domain.redis.ScanPage;
import com.redismanager.domain.redis.ScanRequest;
import com.redismanager.domain.redis.SortedSetEntry;
import com.redismanager.domain.redis.ValueEnvelope;
import com.redismanager.domain.redis.ValueMutation;
import com.redismanager.redis.api.RedisSession;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ObjectOutput;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class LettuceStandaloneRedisSession implements RedisSession {
    private final ConnectionProfile profile;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;

    public LettuceStandaloneRedisSession(
        ConnectionProfile profile,
        RedisClient redisClient,
        StatefulRedisConnection<String, String> connection
    ) {
        this.profile = profile;
        this.redisClient = redisClient;
        this.connection = connection;
    }

    @Override
    public ConnectionProfile profile() {
        return profile;
    }

    @Override
    public CompletionStage<PingResult> ping() {
        return CompletableFuture.supplyAsync(() -> new PingResult(true, connection.sync().ping()));
    }

    @Override
    public CompletionStage<ScanPage<KeySummary>> scan(ScanRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ScanArgs scanArgs = new ScanArgs();
            if (request.matchPattern() != null && !request.matchPattern().isBlank()) {
                scanArgs.match(request.matchPattern().trim());
            }
            if (request.count() > 0) {
                scanArgs.limit(request.count());
            }

            KeyScanCursor<String> cursor;
            if (request.cursor() == null || request.cursor().isBlank() || "0".equals(request.cursor())) {
                cursor = connection.sync().scan(scanArgs);
            } else {
                cursor = connection.sync().scan(ScanCursor.of(request.cursor()), scanArgs);
            }

            List<KeySummary> summaries = cursor.getKeys().stream()
                .map(this::summarizeKey)
                .toList();

            return new ScanPage<>(cursor.getCursor(), cursor.isFinished(), summaries);
        });
    }

    @Override
    public CompletionStage<KeyType> type(String key) {
        return CompletableFuture.supplyAsync(() -> mapType(connection.sync().type(key)));
    }

    @Override
    public CompletionStage<ValueEnvelope> loadValue(String key) {
        return CompletableFuture.supplyAsync(() -> {
            KeyType type = mapType(connection.sync().type(key));
            long ttl = connection.sync().ttl(key);

            if (type == KeyType.STRING) {
                String value = connection.sync().get(key);
                return new ValueEnvelope(key, type, ttl, value, Instant.now(), false);
            }
            if (type == KeyType.HASH) {
                Map<String, String> value = new LinkedHashMap<>(connection.sync().hgetall(key));
                return new ValueEnvelope(key, type, ttl, value, Instant.now(), false);
            }
            if (type == KeyType.LIST) {
                List<String> value = connection.sync().lrange(key, 0, -1);
                return new ValueEnvelope(key, type, ttl, value, Instant.now(), false);
            }
            if (type == KeyType.SET) {
                List<String> value = connection.sync().smembers(key).stream().toList();
                return new ValueEnvelope(key, type, ttl, value, Instant.now(), false);
            }
            if (type == KeyType.ZSET) {
                List<SortedSetEntry> value = connection.sync().zrangeWithScores(key, 0, -1).stream()
                    .map(entry -> new SortedSetEntry(entry.getValue(), entry.getScore()))
                    .toList();
                return new ValueEnvelope(key, type, ttl, value, Instant.now(), false);
            }
            throw new UnsupportedOperationException("当前仅实现 String、Hash、List、Set 和 ZSet 类型读取");
        });
    }

    @Override
    public CompletionStage<Void> saveValue(ValueMutation mutation) {
        return CompletableFuture.runAsync(() -> {
            if (mutation.type() == KeyType.STRING) {
                connection.sync().set(mutation.key(), String.valueOf(mutation.payload()));
                return;
            }
            if (mutation.type() == KeyType.HASH) {
                @SuppressWarnings("unchecked")
                Map<String, String> payload = new LinkedHashMap<>((Map<String, String>) mutation.payload());
                connection.sync().del(mutation.key());
                if (!payload.isEmpty()) {
                    connection.sync().hset(mutation.key(), payload);
                }
                return;
            }
            if (mutation.type() == KeyType.LIST) {
                @SuppressWarnings("unchecked")
                List<String> payload = (List<String>) mutation.payload();
                connection.sync().del(mutation.key());
                if (!payload.isEmpty()) {
                    connection.sync().rpush(mutation.key(), payload.toArray(String[]::new));
                }
                return;
            }
            if (mutation.type() == KeyType.SET) {
                @SuppressWarnings("unchecked")
                List<String> payload = (List<String>) mutation.payload();
                connection.sync().del(mutation.key());
                if (!payload.isEmpty()) {
                    connection.sync().sadd(mutation.key(), payload.toArray(String[]::new));
                }
                return;
            }
            if (mutation.type() == KeyType.ZSET) {
                @SuppressWarnings("unchecked")
                List<SortedSetEntry> payload = (List<SortedSetEntry>) mutation.payload();
                connection.sync().del(mutation.key());
                if (!payload.isEmpty()) {
                    ScoredValue<String>[] values = payload.stream()
                        .map(entry -> ScoredValue.just(entry.score(), entry.member()))
                        .toArray(ScoredValue[]::new);
                    connection.sync().zadd(mutation.key(), values);
                }
                return;
            }
            throw new UnsupportedOperationException("当前仅实现 String、Hash、List、Set 和 ZSet 类型写入");
        });
    }

    @Override
    public CompletionStage<Boolean> expire(String key, long ttlSeconds) {
        return CompletableFuture.supplyAsync(() -> connection.sync().expire(key, ttlSeconds));
    }

    @Override
    public CompletionStage<Boolean> persist(String key) {
        return CompletableFuture.supplyAsync(() -> connection.sync().persist(key));
    }

    @Override
    public CompletionStage<Long> deleteKeys(List<String> keys) {
        return CompletableFuture.supplyAsync(() -> connection.sync().del(keys.toArray(String[]::new)));
    }

    @Override
    public CompletionStage<CommandResult> execute(CommandRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long startedAt = System.currentTimeMillis();

            try {
                ProtocolKeyword keyword = protocolKeyword(request.command());
                CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
                for (String argument : request.arguments()) {
                    args.add(argument);
                }

                Object result = connection.sync().dispatch(keyword, new ObjectOutput<>(StringCodec.UTF8), args);
                long durationMs = System.currentTimeMillis() - startedAt;
                return new CommandResult(true, formatResult(result), durationMs, null);
            } catch (RuntimeException exception) {
                long durationMs = System.currentTimeMillis() - startedAt;
                return new CommandResult(false, "", durationMs, defaultMessage(exception));
            }
        });
    }

    @Override
    public void close() {
        try {
            connection.close();
        } finally {
            redisClient.shutdown();
        }
    }

    private KeyType mapType(String type) {
        if (type == null) {
            return KeyType.UNKNOWN;
        }

        return switch (type.toLowerCase()) {
            case "string" -> KeyType.STRING;
            case "hash" -> KeyType.HASH;
            case "list" -> KeyType.LIST;
            case "set" -> KeyType.SET;
            case "zset" -> KeyType.ZSET;
            case "stream" -> KeyType.STREAM;
            default -> KeyType.UNKNOWN;
        };
    }

    private KeySummary summarizeKey(String key) {
        String rawType = connection.sync().type(key);
        long ttl = connection.sync().ttl(key);
        return new KeySummary(key, mapType(rawType), ttl, "-", -1);
    }

    private ProtocolKeyword protocolKeyword(String command) {
        String normalized = command.trim().toUpperCase();
        try {
            return CommandType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return new ProtocolKeyword() {
                @Override
                public byte[] getBytes() {
                    return normalized.getBytes(StandardCharsets.UTF_8);
                }

                @Override
                public String name() {
                    return normalized;
                }
            };
        }
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "(nil)";
        }
        if (result instanceof List<?> list) {
            if (list.isEmpty()) {
                return "(empty list)";
            }

            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < list.size(); index++) {
                builder.append(index + 1)
                    .append(". ")
                    .append(formatResult(list.get(index)));
                if (index < list.size() - 1) {
                    builder.append(System.lineSeparator());
                }
            }
            return builder.toString();
        }
        if (result instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                return "(empty map)";
            }

            StringBuilder builder = new StringBuilder();
            int index = 1;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                builder.append(index++)
                    .append(". ")
                    .append(String.valueOf(entry.getKey()))
                    .append(" => ")
                    .append(formatResult(entry.getValue()))
                    .append(System.lineSeparator());
            }
            return builder.toString().trim();
        }
        return String.valueOf(result);
    }

    private String defaultMessage(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}

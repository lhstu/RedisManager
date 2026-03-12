package com.redismanager.redis.lettuce;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.domain.connection.ConnectionProfileDraft;
import io.lettuce.core.RedisURI;

import java.time.Duration;

public final class LettuceSupport {
    private LettuceSupport() {
    }

    public static RedisURI toRedisUri(ConnectionProfileDraft draft) {
        RedisURI redisUri = RedisURI.Builder.redis(draft.host().trim(), draft.port())
            .withDatabase(draft.database())
            .withSsl(draft.sslEnabled())
            .withTimeout(Duration.ofMillis(draft.connectTimeoutMs()))
            .build();

        if (draft.username() != null && !draft.username().isBlank()) {
            redisUri.setUsername(draft.username().trim());
        }
        if (draft.passwordRef() != null && !draft.passwordRef().isBlank()) {
            redisUri.setPassword(draft.passwordRef().trim().toCharArray());
        }
        return redisUri;
    }

    public static RedisURI toRedisUri(ConnectionProfile profile) {
        return toRedisUri(new ConnectionProfileDraft(
            profile.name(),
            profile.host(),
            profile.port(),
            profile.username(),
            profile.passwordRef(),
            profile.database(),
            profile.sslEnabled(),
            profile.connectTimeoutMs(),
            profile.readOnly(),
            profile.tags()
        ));
    }
}

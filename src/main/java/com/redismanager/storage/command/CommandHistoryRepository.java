package com.redismanager.storage.command;

import com.redismanager.domain.redis.CommandHistoryEntry;

import java.util.List;
import java.util.UUID;

public interface CommandHistoryRepository {
    List<CommandHistoryEntry> findRecentByProfileId(UUID profileId, int limit);

    CommandHistoryEntry save(UUID profileId, String commandText, long durationMs, boolean success);

    void deleteByProfileId(UUID profileId);
}

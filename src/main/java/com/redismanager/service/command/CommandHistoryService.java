package com.redismanager.service.command;

import com.redismanager.domain.redis.CommandHistoryEntry;

import java.util.List;
import java.util.UUID;

public interface CommandHistoryService {
    List<CommandHistoryEntry> recentCommands(UUID profileId, int limit);

    CommandHistoryEntry record(UUID profileId, String commandText, long durationMs, boolean success);

    void clear(UUID profileId);
}

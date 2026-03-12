package com.redismanager.service.command;

import com.redismanager.domain.redis.CommandHistoryEntry;
import com.redismanager.storage.command.CommandHistoryRepository;

import java.util.List;
import java.util.UUID;

public final class DefaultCommandHistoryService implements CommandHistoryService {
    private final CommandHistoryRepository repository;

    public DefaultCommandHistoryService(CommandHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CommandHistoryEntry> recentCommands(UUID profileId, int limit) {
        return repository.findRecentByProfileId(profileId, limit);
    }

    @Override
    public CommandHistoryEntry record(UUID profileId, String commandText, long durationMs, boolean success) {
        return repository.save(profileId, commandText, durationMs, success);
    }

    @Override
    public void clear(UUID profileId) {
        repository.deleteByProfileId(profileId);
    }
}

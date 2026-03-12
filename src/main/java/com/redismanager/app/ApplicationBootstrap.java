package com.redismanager.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redismanager.service.connection.ConnectionProfileService;
import com.redismanager.service.connection.ConnectionSessionService;
import com.redismanager.service.connection.ConnectionTestService;
import com.redismanager.service.command.CommandHistoryService;
import com.redismanager.service.command.DefaultCommandHistoryService;
import com.redismanager.service.connection.DefaultConnectionSessionService;
import com.redismanager.service.connection.DefaultConnectionProfileService;
import com.redismanager.service.connection.LettuceConnectionTestService;
import com.redismanager.storage.DatabaseManager;
import com.redismanager.storage.command.CommandHistoryRepository;
import com.redismanager.storage.command.SqliteCommandHistoryRepository;
import com.redismanager.storage.connection.ConnectionProfileRepository;
import com.redismanager.storage.connection.SqliteConnectionProfileRepository;

public final class ApplicationBootstrap {
    private final DatabaseManager databaseManager;
    private final ObjectMapper objectMapper;

    public ApplicationBootstrap() {
        this.databaseManager = DatabaseManager.createDefault();
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public void initialize() {
        databaseManager.initialize();
    }

    public ConnectionProfileRepository connectionProfileRepository() {
        return new SqliteConnectionProfileRepository(databaseManager, objectMapper);
    }

    public CommandHistoryRepository commandHistoryRepository() {
        return new SqliteCommandHistoryRepository(databaseManager);
    }

    public ConnectionProfileService connectionProfileService() {
        return new DefaultConnectionProfileService(connectionProfileRepository());
    }

    public ConnectionTestService connectionTestService() {
        return new LettuceConnectionTestService();
    }

    public ConnectionSessionService connectionSessionService() {
        return new DefaultConnectionSessionService();
    }

    public CommandHistoryService commandHistoryService() {
        return new DefaultCommandHistoryService(commandHistoryRepository());
    }
}

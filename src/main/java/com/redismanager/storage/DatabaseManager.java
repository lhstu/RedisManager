package com.redismanager.storage;

import com.redismanager.support.AppDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String JDBC_PREFIX = "jdbc:sqlite:";

    private final Path databasePath;

    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath;
    }

    public static DatabaseManager createDefault() {
        return new DatabaseManager(AppDirectories.databasePath());
    }

    public void initialize() {
        try {
            AppDirectories.ensureCreated();
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建应用数据目录", exception);
        }

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute(Migrations.createConnectionProfilesTable());
            statement.execute(Migrations.createCommandHistoryTable());
            statement.execute(Migrations.createAppSettingsTable());
            log.info("SQLite schema initialized at {}", databasePath);
        } catch (SQLException exception) {
            throw new IllegalStateException("初始化 SQLite 数据库失败", exception);
        }
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_PREFIX + databasePath);
    }
}

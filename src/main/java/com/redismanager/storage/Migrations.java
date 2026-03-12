package com.redismanager.storage;

public final class Migrations {
    private Migrations() {
    }

    public static String createConnectionProfilesTable() {
        return """
            CREATE TABLE IF NOT EXISTS connection_profiles (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                host TEXT NOT NULL,
                port INTEGER NOT NULL,
                username TEXT,
                password_ref TEXT,
                database_index INTEGER NOT NULL DEFAULT 0,
                ssl_enabled INTEGER NOT NULL DEFAULT 0,
                connect_timeout_ms INTEGER NOT NULL DEFAULT 3000,
                read_only INTEGER NOT NULL DEFAULT 0,
                tags_json TEXT NOT NULL DEFAULT '[]',
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
            """;
    }

    public static String createCommandHistoryTable() {
        return """
            CREATE TABLE IF NOT EXISTS command_history (
                id TEXT PRIMARY KEY,
                profile_id TEXT NOT NULL,
                command_text TEXT NOT NULL,
                executed_at TEXT NOT NULL,
                duration_ms INTEGER NOT NULL,
                success INTEGER NOT NULL,
                FOREIGN KEY(profile_id) REFERENCES connection_profiles(id)
            )
            """;
    }

    public static String createAppSettingsTable() {
        return """
            CREATE TABLE IF NOT EXISTS app_settings (
                setting_key TEXT PRIMARY KEY,
                setting_value TEXT NOT NULL
            )
            """;
    }
}

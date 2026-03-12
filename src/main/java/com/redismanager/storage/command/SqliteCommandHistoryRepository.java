package com.redismanager.storage.command;

import com.redismanager.domain.redis.CommandHistoryEntry;
import com.redismanager.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SqliteCommandHistoryRepository implements CommandHistoryRepository {
    private final DatabaseManager databaseManager;

    public SqliteCommandHistoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<CommandHistoryEntry> findRecentByProfileId(UUID profileId, int limit) {
        String sql = """
            SELECT id, profile_id, command_text, executed_at, duration_ms, success
            FROM command_history
            WHERE profile_id = ?
            ORDER BY executed_at DESC
            LIMIT ?
            """;

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, profileId.toString());
            statement.setInt(2, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<CommandHistoryEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    entries.add(mapRow(resultSet));
                }
                return List.copyOf(entries);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("查询命令历史失败", exception);
        }
    }

    @Override
    public CommandHistoryEntry save(UUID profileId, String commandText, long durationMs, boolean success) {
        UUID id = UUID.randomUUID();
        Instant executedAt = Instant.now();

        String sql = """
            INSERT INTO command_history (id, profile_id, command_text, executed_at, duration_ms, success)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.setString(2, profileId.toString());
            statement.setString(3, commandText);
            statement.setTimestamp(4, Timestamp.from(executedAt));
            statement.setLong(5, durationMs);
            statement.setBoolean(6, success);
            statement.executeUpdate();
            return new CommandHistoryEntry(id, profileId, commandText, executedAt, durationMs, success);
        } catch (SQLException exception) {
            throw new IllegalStateException("保存命令历史失败", exception);
        }
    }

    @Override
    public void deleteByProfileId(UUID profileId) {
        String sql = "DELETE FROM command_history WHERE profile_id = ?";

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, profileId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("清空命令历史失败", exception);
        }
    }

    private CommandHistoryEntry mapRow(ResultSet resultSet) throws SQLException {
        return new CommandHistoryEntry(
            UUID.fromString(resultSet.getString("id")),
            UUID.fromString(resultSet.getString("profile_id")),
            resultSet.getString("command_text"),
            resultSet.getTimestamp("executed_at").toInstant(),
            resultSet.getLong("duration_ms"),
            resultSet.getBoolean("success")
        );
    }
}

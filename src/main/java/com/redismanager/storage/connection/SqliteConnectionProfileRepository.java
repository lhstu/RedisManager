package com.redismanager.storage.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.domain.connection.ConnectionProfileDraft;
import com.redismanager.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteConnectionProfileRepository implements ConnectionProfileRepository {
    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() {
    };

    private final DatabaseManager databaseManager;
    private final ObjectMapper objectMapper;

    public SqliteConnectionProfileRepository(DatabaseManager databaseManager, ObjectMapper objectMapper) {
        this.databaseManager = databaseManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ConnectionProfile> findAll() {
        String sql = """
            SELECT id, name, host, port, username, password_ref, database_index, ssl_enabled,
                   connect_timeout_ms, read_only, tags_json, created_at, updated_at
            FROM connection_profiles
            ORDER BY updated_at DESC
            """;

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<ConnectionProfile> results = new java.util.ArrayList<>();
            while (resultSet.next()) {
                results.add(mapRow(resultSet));
            }
            return List.copyOf(results);
        } catch (SQLException exception) {
            throw new IllegalStateException("查询连接配置失败", exception);
        }
    }

    @Override
    public Optional<ConnectionProfile> findById(UUID id) {
        String sql = """
            SELECT id, name, host, port, username, password_ref, database_index, ssl_enabled,
                   connect_timeout_ms, read_only, tags_json, created_at, updated_at
            FROM connection_profiles
            WHERE id = ?
            """;

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("按 ID 查询连接配置失败", exception);
        }
    }

    @Override
    public ConnectionProfile save(ConnectionProfileDraft draft) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        String sql = """
            INSERT INTO connection_profiles (
                id, name, host, port, username, password_ref, database_index, ssl_enabled,
                connect_timeout_ms, read_only, tags_json, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindDraft(statement, id, draft, now, now);
            statement.executeUpdate();
            return findById(id).orElseThrow(() -> new IllegalStateException("保存后读取连接配置失败"));
        } catch (SQLException exception) {
            throw new IllegalStateException("保存连接配置失败", exception);
        }
    }

    @Override
    public ConnectionProfile update(UUID id, ConnectionProfileDraft draft) {
        ConnectionProfile existing = findById(id)
            .orElseThrow(() -> new IllegalArgumentException("连接配置不存在: " + id));

        String sql = """
            UPDATE connection_profiles
            SET name = ?, host = ?, port = ?, username = ?, password_ref = ?, database_index = ?,
                ssl_enabled = ?, connect_timeout_ms = ?, read_only = ?, tags_json = ?, updated_at = ?
            WHERE id = ?
            """;

        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setString(index++, draft.name());
            statement.setString(index++, draft.host());
            statement.setInt(index++, draft.port());
            statement.setString(index++, draft.username());
            statement.setString(index++, draft.passwordRef());
            statement.setInt(index++, draft.database());
            statement.setBoolean(index++, draft.sslEnabled());
            statement.setInt(index++, draft.connectTimeoutMs());
            statement.setBoolean(index++, draft.readOnly());
            statement.setString(index++, serializeTags(draft.tags()));
            statement.setTimestamp(index++, Timestamp.from(Instant.now()));
            statement.setString(index, existing.id().toString());
            statement.executeUpdate();
            return findById(id).orElseThrow(() -> new IllegalStateException("更新后读取连接配置失败"));
        } catch (SQLException exception) {
            throw new IllegalStateException("更新连接配置失败", exception);
        }
    }

    @Override
    public void delete(UUID id) {
        String sql = "DELETE FROM connection_profiles WHERE id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("删除连接配置失败", exception);
        }
    }

    private void bindDraft(
        PreparedStatement statement,
        UUID id,
        ConnectionProfileDraft draft,
        Instant createdAt,
        Instant updatedAt
    ) throws SQLException {
        int index = 1;
        statement.setString(index++, id.toString());
        statement.setString(index++, draft.name());
        statement.setString(index++, draft.host());
        statement.setInt(index++, draft.port());
        statement.setString(index++, draft.username());
        statement.setString(index++, draft.passwordRef());
        statement.setInt(index++, draft.database());
        statement.setBoolean(index++, draft.sslEnabled());
        statement.setInt(index++, draft.connectTimeoutMs());
        statement.setBoolean(index++, draft.readOnly());
        statement.setString(index++, serializeTags(draft.tags()));
        statement.setTimestamp(index++, Timestamp.from(createdAt));
        statement.setTimestamp(index, Timestamp.from(updatedAt));
    }

    private ConnectionProfile mapRow(ResultSet resultSet) throws SQLException {
        return new ConnectionProfile(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name"),
            resultSet.getString("host"),
            resultSet.getInt("port"),
            resultSet.getString("username"),
            resultSet.getString("password_ref"),
            resultSet.getInt("database_index"),
            resultSet.getBoolean("ssl_enabled"),
            resultSet.getInt("connect_timeout_ms"),
            resultSet.getBoolean("read_only"),
            deserializeTags(resultSet.getString("tags_json")),
            resultSet.getTimestamp("created_at").toInstant(),
            resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private String serializeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? List.of() : tags);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化标签失败", exception);
        }
    }

    private List<String> deserializeTags(String json) {
        try {
            return objectMapper.readValue(json, TAGS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("反序列化标签失败", exception);
        }
    }
}

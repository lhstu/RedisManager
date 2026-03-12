package com.redismanager.storage.connection;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.domain.connection.ConnectionProfileDraft;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionProfileRepository {
    List<ConnectionProfile> findAll();

    Optional<ConnectionProfile> findById(UUID id);

    ConnectionProfile save(ConnectionProfileDraft draft);

    ConnectionProfile update(UUID id, ConnectionProfileDraft draft);

    void delete(UUID id);
}

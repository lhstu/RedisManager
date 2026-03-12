package com.redismanager.service.connection;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.domain.connection.ConnectionProfileDraft;

import java.util.List;
import java.util.UUID;

public interface ConnectionProfileService {
    List<ConnectionProfile> listProfiles();

    ConnectionProfile createProfile(ConnectionProfileDraft draft);

    ConnectionProfile updateProfile(UUID id, ConnectionProfileDraft draft);

    void deleteProfile(UUID id);
}

package com.redismanager.service.connection;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.domain.connection.ConnectionProfileDraft;
import com.redismanager.storage.connection.ConnectionProfileRepository;

import java.util.List;
import java.util.UUID;

public final class DefaultConnectionProfileService implements ConnectionProfileService {
    private final ConnectionProfileRepository repository;

    public DefaultConnectionProfileService(ConnectionProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ConnectionProfile> listProfiles() {
        return repository.findAll();
    }

    @Override
    public ConnectionProfile createProfile(ConnectionProfileDraft draft) {
        validate(draft);
        return repository.save(normalize(draft));
    }

    @Override
    public ConnectionProfile updateProfile(UUID id, ConnectionProfileDraft draft) {
        validate(draft);
        return repository.update(id, normalize(draft));
    }

    @Override
    public void deleteProfile(UUID id) {
        repository.delete(id);
    }

    private void validate(ConnectionProfileDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("连接配置不能为空");
        }
        if (isBlank(draft.name())) {
            throw new IllegalArgumentException("连接名称不能为空");
        }
        if (isBlank(draft.host())) {
            throw new IllegalArgumentException("主机地址不能为空");
        }
        if (draft.port() < 1 || draft.port() > 65535) {
            throw new IllegalArgumentException("端口必须在 1 到 65535 之间");
        }
        if (draft.database() < 0) {
            throw new IllegalArgumentException("数据库编号不能为负数");
        }
        if (draft.connectTimeoutMs() < 500) {
            throw new IllegalArgumentException("连接超时不能小于 500 毫秒");
        }
    }

    private ConnectionProfileDraft normalize(ConnectionProfileDraft draft) {
        return new ConnectionProfileDraft(
            draft.name().trim(),
            draft.host().trim(),
            draft.port(),
            blankToNull(draft.username()),
            blankToNull(draft.passwordRef()),
            draft.database(),
            draft.sslEnabled(),
            draft.connectTimeoutMs(),
            draft.readOnly(),
            draft.tags() == null ? List.of() : draft.tags().stream().map(String::trim).filter(tag -> !tag.isEmpty()).toList()
        );
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

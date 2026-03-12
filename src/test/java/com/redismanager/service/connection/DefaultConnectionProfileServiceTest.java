package com.redismanager.service.connection;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.domain.connection.ConnectionProfileDraft;
import com.redismanager.storage.connection.ConnectionProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultConnectionProfileServiceTest {
    @Test
    void shouldTrimAndPersistConnectionProfile() {
        InMemoryConnectionProfileRepository repository = new InMemoryConnectionProfileRepository();
        DefaultConnectionProfileService service = new DefaultConnectionProfileService(repository);

        ConnectionProfile profile = service.createProfile(new ConnectionProfileDraft(
            " 开发环境 ",
            " 127.0.0.1 ",
            6379,
            " ",
            " ",
            0,
            false,
            3000,
            false,
            List.of(" dev ", " local ")
        ));

        assertEquals("开发环境", profile.name());
        assertEquals("127.0.0.1", profile.host());
        assertEquals(List.of("dev", "local"), profile.tags());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void shouldRejectInvalidPort() {
        InMemoryConnectionProfileRepository repository = new InMemoryConnectionProfileRepository();
        DefaultConnectionProfileService service = new DefaultConnectionProfileService(repository);

        assertThrows(IllegalArgumentException.class, () -> service.createProfile(new ConnectionProfileDraft(
            "test",
            "127.0.0.1",
            70000,
            null,
            null,
            0,
            false,
            3000,
            false,
            List.of()
        )));
    }

    private static final class InMemoryConnectionProfileRepository implements ConnectionProfileRepository {
        private final List<ConnectionProfile> profiles = new ArrayList<>();

        @Override
        public List<ConnectionProfile> findAll() {
            return List.copyOf(profiles);
        }

        @Override
        public Optional<ConnectionProfile> findById(UUID id) {
            return profiles.stream().filter(profile -> profile.id().equals(id)).findFirst();
        }

        @Override
        public ConnectionProfile save(ConnectionProfileDraft draft) {
            ConnectionProfile profile = new ConnectionProfile(
                UUID.randomUUID(),
                draft.name(),
                draft.host(),
                draft.port(),
                draft.username(),
                draft.passwordRef(),
                draft.database(),
                draft.sslEnabled(),
                draft.connectTimeoutMs(),
                draft.readOnly(),
                draft.tags(),
                Instant.now(),
                Instant.now()
            );
            profiles.add(profile);
            return profile;
        }

        @Override
        public ConnectionProfile update(UUID id, ConnectionProfileDraft draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(UUID id) {
            throw new UnsupportedOperationException();
        }
    }
}

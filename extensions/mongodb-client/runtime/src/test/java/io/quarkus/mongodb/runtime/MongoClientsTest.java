package io.quarkus.mongodb.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.mongodb.ConnectionString;

class MongoClientsTest {

    private final MongoClientConfig config = mock(MongoClientConfig.class);
    private final CredentialConfig credentials = mock(CredentialConfig.class);

    MongoClientsTest() {
        when(config.credentials()).thenReturn(credentials);
        when(credentials.authSource()).thenReturn(Optional.empty());
        when(config.database()).thenReturn(Optional.empty());
    }

    @Test
    void shouldUseExplicitAuthSource() {
        when(credentials.authSource()).thenReturn(Optional.of("credentials-db"));
        when(config.database()).thenReturn(Optional.of("configured-db"));

        assertThat(MongoClients.getAuthSource(config,
                new ConnectionString("mongodb://localhost/connection-db?authSource=connection-auth-db")))
                .isEqualTo("credentials-db");
    }

    @Test
    void shouldUseAuthSourceFromConnectionString() {
        when(config.database()).thenReturn(Optional.of("configured-db"));

        assertThat(MongoClients.getAuthSource(config,
                new ConnectionString("mongodb://localhost/connection-db?authSource=connection-auth-db")))
                .isEqualTo("connection-auth-db");
    }

    @Test
    void shouldUseConfiguredDatabaseBeforeConnectionStringDatabase() {
        when(config.database()).thenReturn(Optional.of("configured-db"));

        assertThat(MongoClients.getAuthSource(config, new ConnectionString("mongodb://localhost/connection-db")))
                .isEqualTo("configured-db");
    }

    @Test
    void shouldUseConnectionStringDatabase() {
        assertThat(MongoClients.getAuthSource(config, new ConnectionString("mongodb://localhost/connection-db")))
                .isEqualTo("connection-db");
    }

    @Test
    void shouldUseAdminByDefault() {
        assertThat(MongoClients.getAuthSource(config, new ConnectionString("mongodb://localhost")))
                .isEqualTo("admin");
    }
}

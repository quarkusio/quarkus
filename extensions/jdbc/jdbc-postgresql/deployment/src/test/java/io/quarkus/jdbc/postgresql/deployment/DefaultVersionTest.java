package io.quarkus.jdbc.postgresql.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;

public class DefaultVersionTest {

    @Test
    void defaultVersionMatchesPomProperty() {
        String expectedVersion = System.getProperty("postgres.default.version");
        assertThat(expectedVersion)
                .as("postgres.default.version system property should be set by maven-surefire-plugin")
                .isNotNull();

        String actualVersion = DatabaseVersionLoader.loadDefaultVersion("postgresql");
        assertThat(actualVersion)
                .as("PostgreSQL default version should match the postgres.default.version Maven property")
                .isEqualTo(expectedVersion);
    }
}

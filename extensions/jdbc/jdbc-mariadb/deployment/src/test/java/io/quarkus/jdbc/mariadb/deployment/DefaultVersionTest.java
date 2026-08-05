package io.quarkus.jdbc.mariadb.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;

public class DefaultVersionTest {

    @Test
    void defaultVersionMatchesPomProperty() {
        String expectedVersion = System.getProperty("mariadb.default.version");
        assertThat(expectedVersion)
                .as("mariadb.default.version system property should be set by maven-surefire-plugin")
                .isNotNull();

        String actualVersion = DatabaseVersionLoader.loadDefaultVersion("mariadb");
        assertThat(actualVersion)
                .as("MariaDB default version should match the mariadb.default.version Maven property")
                .isEqualTo(expectedVersion);
    }
}

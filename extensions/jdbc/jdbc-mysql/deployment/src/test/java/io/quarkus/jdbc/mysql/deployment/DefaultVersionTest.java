package io.quarkus.jdbc.mysql.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;

public class DefaultVersionTest {

    @Test
    void defaultVersionMatchesPomProperty() {
        String expectedVersion = System.getProperty("mysql.version");
        assertThat(expectedVersion)
                .as("mysql.version system property should be set by maven-surefire-plugin")
                .isNotNull();

        String actualVersion = DatabaseVersionLoader.loadDefaultVersion("mysql");
        assertThat(actualVersion)
                .as("MySQL default version should match the mysql.version Maven property")
                .isEqualTo(expectedVersion);
    }
}

package io.quarkus.reactive.db2.client.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;

public class DefaultVersionTest {

    @Test
    void defaultVersionMatchesPomProperty() {
        String expectedVersion = System.getProperty("db2.version");
        assertThat(expectedVersion)
                .as("db2.version system property should be set by maven-surefire-plugin")
                .isNotNull();

        String actualVersion = DatabaseVersionLoader.loadDefaultVersion("db2");
        assertThat(actualVersion)
                .as("DB2 default version should match the db2.version Maven property")
                .isEqualTo(expectedVersion);
    }
}

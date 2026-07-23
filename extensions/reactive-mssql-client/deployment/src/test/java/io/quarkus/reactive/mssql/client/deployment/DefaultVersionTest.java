package io.quarkus.reactive.mssql.client.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;

public class DefaultVersionTest {

    @Test
    void defaultVersionMatchesPomProperty() {
        String expectedVersion = System.getProperty("mssql.default.version");
        assertThat(expectedVersion)
                .as("mssql.default.version system property should be set by maven-surefire-plugin")
                .isNotNull();

        String actualVersion = DatabaseVersionLoader.loadDefaultVersion("mssql");
        assertThat(actualVersion)
                .as("MSSQL default version should match the mssql.default.version Maven property")
                .isEqualTo(expectedVersion);
    }
}

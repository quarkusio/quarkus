package io.quarkus.reactive.oracle.client.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;

public class DefaultVersionTest {

    @Test
    void defaultVersionMatchesPomProperty() {
        String expectedVersion = System.getProperty("oracle.version");
        assertThat(expectedVersion)
                .as("oracle.version system property should be set by maven-surefire-plugin")
                .isNotNull();

        String actualVersion = DatabaseVersionLoader.loadDefaultVersion("oracle");
        assertThat(actualVersion)
                .as("Oracle default version should match the oracle.version Maven property")
                .isEqualTo(expectedVersion);
    }
}

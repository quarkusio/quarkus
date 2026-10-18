package io.quarkus.jdbc.h2.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;

public class DefaultVersionTest {

    @Test
    void defaultVersionMatchesPomProperty() {
        String expectedVersion = System.getProperty("h2.version");
        assertThat(expectedVersion)
                .as("h2.version system property should be set by maven-surefire-plugin")
                .isNotNull();

        String actualVersion = DatabaseVersionLoader.loadDefaultVersion("h2");
        assertThat(actualVersion)
                .as("H2 default version should match the h2.version Maven property")
                .isEqualTo(expectedVersion);
    }
}

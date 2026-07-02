package io.quarkus.jdbc.db2.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;

public class DefaultVersionTest {

    @Test
    void defaultVersionMatchesPomProperty() {
        String expectedVersion = System.getProperty("db2.default.version");
        assertThat(expectedVersion)
                .as("db2.default.version system property should be set by maven-surefire-plugin")
                .isNotNull();

        String actualVersion = DatabaseVersionLoader.loadDefaultVersion("db2");
        assertThat(actualVersion)
                .as("DB2 default version should match the db2.default.version Maven property")
                .isEqualTo(expectedVersion);
    }
}

package io.quarkus.reactive.mysql.client.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.deployment.spi.DatabaseVersionLoader;

public class DefaultVersionTest {

    @Test
    void mysqlDefaultVersionMatchesPomProperty() {
        String expectedVersion = System.getProperty("mysql.default.version");
        assertThat(expectedVersion)
                .as("mysql.default.version system property should be set by maven-surefire-plugin")
                .isNotNull();

        String actualVersion = DatabaseVersionLoader.loadDefaultVersion("mysql");
        assertThat(actualVersion)
                .as("MySQL default version should match the mysql.default.version Maven property")
                .isEqualTo(expectedVersion);
    }

    @Test
    void mariadbDefaultVersionMatchesPomProperty() {
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

package io.quarkus.jdbc.postgresql.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class DevServicesPostgresqlDatasourceWaitForConnectionTestCase {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            // PostgreSQL then does not log that it is ready to accept connections, like images that log differently
            // from the official one, so the default strategy, which waits for that message, would time out
            .overrideConfigKey("quarkus.datasource.devservices.command", "-c log_min_messages=fatal")
            .overrideConfigKey("quarkus.datasource.devservices.wait.strategy", "connection");

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void testDatasource() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5)).isTrue();
        }
    }
}

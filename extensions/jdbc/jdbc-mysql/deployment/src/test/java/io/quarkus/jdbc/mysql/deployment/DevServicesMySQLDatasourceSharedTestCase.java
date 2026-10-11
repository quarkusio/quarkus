package io.quarkus.jdbc.mysql.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class DevServicesMySQLDatasourceSharedTestCase {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.datasource.db-kind", "mysql")
            .overrideConfigKey("quarkus.datasource.devservices.shares-container", "true")
            .overrideConfigKey("quarkus.datasource.\"SHARED\".db-kind", "mysql")
            .overrideConfigKey("quarkus.datasource.\"SHARED\".devservices.use-from", "<default>");

    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @Named("SHARED")
    AgroalDataSource sharedDataSource;

    @Test
    public void sharedDatasourceReusesDefaultDatasourceContainer() throws Exception {
        String defaultJdbcUrl = defaultDataSource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration().jdbcUrl();
        String sharedJdbcUrl = sharedDataSource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration().jdbcUrl();

        assertTrue(defaultJdbcUrl.contains("jdbc:mysql:"));
        // Both datasources must resolve to the exact same JDBC URL, meaning they reuse the same
        // Dev Services container, instead of each starting their own.
        assertEquals(defaultJdbcUrl, sharedJdbcUrl);

        try (Connection connection = sharedDataSource.getConnection()) {
        }
    }
}

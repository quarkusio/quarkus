package io.quarkus.jdbc.postgresql.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.test.QuarkusUnitTest;

public class DevServicesPostgresqlDatasourceNamedWithUsernameTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.datasource.\"DB2\".db-kind", "postgresql")
            .overrideConfigKey("quarkus.datasource.\"DB2\".username", "foo")
            .overrideConfigKey("quarkus.datasource.\"DB2\".password", "foo");

    @Inject
    @Named("DB2")
    AgroalDataSource dataSource;

    @Test
    public void testDatasource() throws Exception {
        AgroalConnectionPoolConfiguration configuration = null;

        try {
            configuration = dataSource.getConfiguration().connectionPoolConfiguration();
        } catch (NullPointerException e) {
            // we catch the NPE here as we have a proxycd and we can't test dataSource directly
            fail("Datasource should not be null");
        }
        assertTrue(configuration.connectionFactoryConfiguration().jdbcUrl().contains("jdbc:postgresql:"));
        assertEquals("foo", configuration.connectionFactoryConfiguration().principal().getName());

        try (Connection connection = dataSource.getConnection()) {
        }
    }
}

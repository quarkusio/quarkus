package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class NamedDataSourceConfigTest {
    @Inject
    @DataSource("testing")
    AgroalDataSource ds;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-named-datasource.properties");

    @Test
    public void testNamedDataSourceInjection() throws SQLException {
        dataSourceAssert("testing", ds, "jdbc:h2:tcp://localhost/mem:testing",
                "username-named", 3, 13);
    }

    @Test
    public void testDefaultDataSourceNotCreated() {
        // This test only configures a named datasource; the default datasource should not be created.
        // To run with debug logging for datasource request processing:
        //   ./mvnw verify -f extensions/agroal/deployment/ -Dtest-containers -Dstart-containers \
        //       -Dtest="NamedDataSourceConfigTest#testDefaultDataSourceNotCreated" \
        //       -Dquarkus.log.category.\"io.quarkus.datasource.deployment\".level=DEBUG
        assertThat(Arc.container().select(AgroalDataSource.class).isUnsatisfied()).isTrue();
    }

    private static void dataSourceAssert(String dataSourceName, AgroalDataSource dataSource, String jdbcUrl, String username,
            int minSize, int maxSize)
            throws SQLException {
        AgroalConnectionPoolConfiguration configuration = null;

        try {
            configuration = dataSource.getConfiguration().connectionPoolConfiguration();
        } catch (NullPointerException e) {
            // we catch the NPE here as we have a proxy and we can't test dataSource directly
            fail("Datasource " + dataSourceName + " should not be null");
        }
        assertEquals(jdbcUrl, configuration.connectionFactoryConfiguration().jdbcUrl());
        assertEquals(username, configuration.connectionFactoryConfiguration().principal().getName());
        assertEquals(maxSize, configuration.maxSize());
        assertFalse(dataSource.getConfiguration().metricsEnabled()); // metrics not enabled by default

        try (Connection connection = dataSource.getConnection()) {
        }
    }
}

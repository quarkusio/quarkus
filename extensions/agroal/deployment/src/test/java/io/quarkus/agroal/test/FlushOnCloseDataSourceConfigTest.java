package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalPoolInterceptor;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.test.QuarkusUnitTest;

class FlushOnCloseDataSourceConfigTest {

    private static final Logger LOGGER = Logger.getLogger(FlushOnCloseDataSourceConfigTest.class);

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addClasses(FlushOnCloseDataSourceConfigTest.class)
                    .addClasses(ClientUserTrackerInterceptor.class))
            .withConfigurationResource("base.properties")
            // Setting Compatibility MODE to MySQL to enable all the client-info properties on the connection
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:tcp://localhost/mem:flushing;MODE=MySQL")
            .overrideConfigKey("quarkus.datasource.jdbc.min-size", "1")
            .overrideConfigKey("quarkus.datasource.jdbc.initial-size", "1")
            .overrideConfigKey("quarkus.datasource.jdbc.max-size", "2")
            .overrideConfigKey("quarkus.datasource.jdbc.flush-on-close", "true");

    @Test
    void testFlushOnCloseDataSourceConnection() throws SQLException {
        AgroalConnectionPoolConfiguration configuration = defaultDataSource.getConfiguration().connectionPoolConfiguration();

        assertTrue(configuration.flushOnClose());

        try (Connection connection = defaultDataSource.getConnection()) {
            connection.setClientInfo("ClientUser", "1");
            connection.prepareStatement("SELECT 1").executeQuery();
            try (Connection conn2 = defaultDataSource.getConnection()) {
                conn2.setClientInfo("ClientUser", "2");
                conn2.prepareStatement("SELECT 1").executeQuery();
            }
        }

        try (Connection connection = defaultDataSource.getConnection()) {
            assertEquals("1", connection.getClientInfo("ClientUser"));
            try (Connection conn2 = defaultDataSource.getConnection()) {
                // new connection should not contain any mark ClientUser
                assertNull(conn2.getClientInfo("ClientUser"));
            }
        }
    }

    @Singleton
    static class ClientUserTrackerInterceptor implements AgroalPoolInterceptor {

        @Override
        public void onConnectionAcquire(Connection connection) {
            try {
                LOGGER.info("connection acquired ClientUser:" + connection.getClientInfo("ClientUser"));
            } catch (SQLException e) {
                Assertions.fail(e);
            }
        }

        @Override
        public void onConnectionReturn(Connection connection) {
            try {
                // Connection marked "ClientUser:2" must not return to the pool.
                assertNotEquals("2", connection.getClientInfo("ClientUser"));
                LOGGER.info("connection returned ClientUser:" + connection.getClientInfo("ClientUser"));
            } catch (SQLException e) {
                Assertions.fail(e);
            }
        }
    }
}

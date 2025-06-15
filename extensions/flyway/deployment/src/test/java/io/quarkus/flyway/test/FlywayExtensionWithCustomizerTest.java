package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.flyway.FlywayConfigurationCustomizer;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionWithCustomizerTest {

    @Inject
    AgroalDataSource defaultDataSource;

    static final FlywayH2TestCustomizer customizer = FlywayH2TestCustomizer.withDbName("quarkus-customizer")
            .withPort(11303).withInitSqlFile("src/test/resources/callback-init-data.sql");

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setBeforeAllCustomizer(customizer::startH2)
            .setAfterAllCustomizer(customizer::stopH2)
            .withApplicationRoot((jar) -> jar.addClasses(FlywayH2TestCustomizer.class, AddCallbacksCustomizer.class,
                    FlywayExtensionCallback.class, FlywayExtensionCallback2.class, FlywayExtensionCDICallback.class)
                    .addAsResource("db/migration/V1.0.3__Quarkus_Callback.sql")
                    .addAsResource("config-for-default-datasource-with-customizer-config.properties",
                            "application.properties"));

    @Test
    public void testCustomizer() throws SQLException {
        try (Connection connection = defaultDataSource.getConnection();
                Statement stat = connection.createStatement()) {
            try (ResultSet executeQuery = stat.executeQuery("select COUNT(name) from quarked_callback")) {
                assertTrue(executeQuery.next(), "Table exists but it is empty");
                int count = executeQuery.getInt(1);
                // Expect one row for each callback invoked by Flyway
                int expected = FlywayExtensionCallback.DEFAULT_EVENTS.size()
                        + FlywayExtensionCallback2.DEFAULT_EVENTS.size();
                assertEquals(expected, count);
            }
        }
    }

    @Singleton
    public static class AddCallbacksCustomizer implements FlywayConfigurationCustomizer {

        @Override
        public void customize(FluentConfiguration configuration) {
            configuration.callbacks(new FlywayExtensionCallback(), new FlywayExtensionCallback2(),
                    new FlywayExtensionCDICallback());
        }
    }
}

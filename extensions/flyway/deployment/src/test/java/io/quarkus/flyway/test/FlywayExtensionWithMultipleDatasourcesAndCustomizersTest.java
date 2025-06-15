package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.flyway.FlywayConfigurationCustomizer;
import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionWithMultipleDatasourcesAndCustomizersTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @Named("users")
    AgroalDataSource usersDataSource;

    @Inject
    @Named("inventory")
    AgroalDataSource inventoryDataSource;

    static final FlywayH2TestCustomizer h2ForDefault = FlywayH2TestCustomizer.withDbName("quarkus-default-customizer")
            .withPort(11303).withInitSqlFile("src/test/resources/callback-init-data.sql");

    static final FlywayH2TestCustomizer h2ForUsers = FlywayH2TestCustomizer.withDbName("quarkus-users-customizer")
            .withPort(11304).withInitSqlFile("src/test/resources/callback-init-data.sql");

    static final FlywayH2TestCustomizer h2ForInventory = FlywayH2TestCustomizer
            .withDbName("quarkus-inventory-customizer").withPort(11305)
            .withInitSqlFile("src/test/resources/callback-init-data.sql");

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setBeforeAllCustomizer(new Runnable() {
        @Override
        public void run() {
            h2ForDefault.startH2();
            h2ForUsers.startH2();
            h2ForInventory.startH2();
        }
    }).setAfterAllCustomizer(new Runnable() {
        @Override
        public void run() {
            h2ForDefault.stopH2();
            h2ForUsers.stopH2();
            h2ForInventory.stopH2();
        }
    }).withApplicationRoot((jar) -> jar
            .addClasses(FlywayH2TestCustomizer.class, AddCallbacksCustomizerForDefaultDS.class,
                    FlywayExtensionCallback.class, FlywayExtensionCallback2.class, FlywayExtensionCDICallback.class)
            .addAsResource("db/migration/V1.0.3__Quarkus_Callback.sql").addAsResource(
                    "config-for-multiple-datasource-with-customizers-config.properties", "application.properties"));

    @Test
    public void testCustomizers() throws SQLException {
        assertEventCount(defaultDataSource, FlywayExtensionCallback.DEFAULT_EVENTS.size());
        assertEventCount(usersDataSource, FlywayExtensionCallback2.DEFAULT_EVENTS.size());
        assertEventCount(inventoryDataSource, 0);
    }

    private void assertEventCount(AgroalDataSource dataSource, int expectedEventCount) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement stat = connection.createStatement()) {
            try (ResultSet executeQuery = stat.executeQuery("select COUNT(name) from quarked_callback")) {
                assertTrue(executeQuery.next(), "Table exists but it is empty");
                int count = executeQuery.getInt(1);
                assertEquals(expectedEventCount, count);
            }
        }
    }

    @Singleton
    public static class AddCallbacksCustomizerForDefaultDS implements FlywayConfigurationCustomizer {

        @Override
        public void customize(FluentConfiguration configuration) {
            configuration.callbacks(new FlywayExtensionCallback());
        }
    }

    @Singleton
    @FlywayDataSource("users")
    public static class AddCallbacksCustomizerForUsersDS implements FlywayConfigurationCustomizer {

        @Override
        public void customize(FluentConfiguration configuration) {
            configuration.callbacks(new FlywayExtensionCallback2());
        }
    }
}

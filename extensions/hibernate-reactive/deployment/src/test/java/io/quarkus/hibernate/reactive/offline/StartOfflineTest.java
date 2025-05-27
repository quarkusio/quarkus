package io.quarkus.hibernate.reactive.offline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.internal.ConfigurationServiceImpl;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy;
import org.hibernate.reactive.common.spi.Implementor;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.service.ServiceRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.hibernate.orm.runtime.service.QuarkusRuntimeInitDialectFactory;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that an application can be configured to start successfully
 * even if the database is offline when the application starts.
 */
public class StartOfflineTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Hero.class)
                    .addAsResource("application-start-offline.properties", "application.properties"))
            .setLogRecordPredicate(record -> GlobalTemporaryTableStrategy.class.getName().equals(record.getLoggerName())
                    || record.getLoggerName().contains("JdbcEnvironmentInitiator"))
            .assertLogRecords(records -> {
                assertThat(records) // JdbcSettings.ALLOW_METADATA_ON_BOOT
                        .extracting(LogRecord::getMessage)
                        .doesNotContain("HHH000342: Could not obtain connection to query JDBC database metadata");
                assertThat(records) // GlobalTemporaryTableStrategy.CREATE_ID_TABLES
                        .extracting(LogRecord::getMessage).doesNotContain("Unable obtain JDBC Connection");
            });

    @Inject
    Mutiny.SessionFactory factory;

    @Test
    public void applicationStarts() {
        assertThat(factory).isNotNull();
    }

    @Test
    public void testVersionCheckShouldBeDisabledWhenOffline() {
        Implementor sfi = (Implementor) ClientProxy.unwrap(factory);
        ServiceRegistry registry = sfi.getServiceRegistry();

        QuarkusRuntimeInitDialectFactory service = (QuarkusRuntimeInitDialectFactory) registry.getService(DialectFactory.class);
        assertThat(service.isVersionCheckEnabled()).isFalse();
    }

    @Test
    public void testUnitSchemaManagementStrategyIsNone() {
        Object strategy = ((Implementor) ClientProxy.unwrap(factory))
                .getServiceRegistry()
                .getService(ConfigurationServiceImpl.class)
                .getSettings()
                .get(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION);
        assertThat(strategy).isEqualTo("none");
    }
}

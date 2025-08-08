package io.quarkus.hibernate.orm.offline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.LogRecord;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.internal.ConfigurationServiceImpl;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.runtime.service.QuarkusRuntimeInitDialectFactory;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that an application can be configured to start successfully
 * even if the database is offline when the application starts.
 */
public class StartOfflineTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-start-offline.properties", "application.properties"))
            .setLogRecordPredicate(record -> GlobalTemporaryTableStrategy.class.getName().equals(record.getLoggerName())
                    || record.getLoggerName().contains("JdbcEnvironmentInitiator"))
            .assertLogRecords(records -> {
                assertThat(records) // JdbcSettings.ALLOW_METADATA_ON_BOOT
                        .extracting(LogRecord::getMessage)
                        .doesNotContain("HHH000342: Could not obtain connection to query JDBC database metadata");
                assertThat(records) // Local TemporaryTable Strategy
                        .extracting(LogRecord::getMessage).doesNotContain("Unable obtain JDBC Connection");
            });

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Test
    public void applicationStarts() {
        assertThat(entityManagerFactory).isNotNull();
    }

    @Test
    public void testVersionCheckShouldBeDisabledWhenOffline() {
        SessionFactoryImplementor sfi = (SessionFactoryImplementor) entityManagerFactory.unwrap(SessionFactory.class);
        ServiceRegistryImplementor registry = sfi.getServiceRegistry();

        QuarkusRuntimeInitDialectFactory service = (QuarkusRuntimeInitDialectFactory) registry.getService(DialectFactory.class);
        assertThat(service.isVersionCheckEnabled()).isFalse();
    }

    @Test
    public void testUnitSchemaManagementStrategyIsNone() {
        Object strategy = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry()
                .getService(ConfigurationServiceImpl.class)
                .getSettings()
                .get(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION);
        assertThat(strategy).isEqualTo("none");
    }
}

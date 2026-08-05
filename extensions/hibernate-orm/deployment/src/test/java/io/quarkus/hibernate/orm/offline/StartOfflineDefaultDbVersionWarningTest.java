package io.quarkus.hibernate.orm.offline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

public class StartOfflineDefaultDbVersionWarningTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:5432/test")
            // Do NOT set db-version explicitly - let it default
            .overrideConfigKey("quarkus.hibernate-orm.database.start-offline", "true")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none")
            .setLogRecordPredicate(record -> record.getMessage() != null
                    && record.getMessage().contains("default database version"))
            .assertLogRecords(records -> assertThat(records)
                    .as("Warning about default db-version with start-offline")
                    .hasSizeGreaterThanOrEqualTo(1)
                    .anySatisfy(record -> {
                        assertThat(record.getLevel()).isEqualTo(Level.WARNING);
                        assertThat(LOG_FORMATTER.formatMessage(record))
                                .contains("configured to start offline")
                                .contains("default database version")
                                .contains("quarkus.datasource.db-version");
                    }));

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Test
    public void applicationStarts() {
        // Should start but with warning
        assertThat(entityManagerFactory).isNotNull();
    }
}

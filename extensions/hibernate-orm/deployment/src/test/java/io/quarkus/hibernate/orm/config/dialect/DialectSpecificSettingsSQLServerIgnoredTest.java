package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.LogRecord;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil;
import io.quarkus.test.QuarkusUnitTest;

public class DialectSpecificSettingsSQLServerIgnoredTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-start-offline-mssql-dialect.properties", "application.properties"))
            .overrideConfigKey("quarkus.datasource.db-kind", "") // This will override to default which is H2
            .setLogRecordPredicate(record -> HibernateProcessorUtil.class.getName().equals(record.getLoggerName()))
            .overrideConfigKey("quarkus.hibernate-orm.dialect.mssql.compatibility-level", "170") // this will be ignored
            .setLogRecordPredicate(record -> HibernateProcessorUtil.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .anyMatch(l -> l.contains(
                                "SQL Server specific settings being ignored because the database is not SQL Server."));
            });

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Test
    public void applicationStarts() {
        assertThat(entityManagerFactory.getProperties().get("hibernate.dialect.sqlserver.compatibility_level"))
                .isEqualTo(null);
    }
}

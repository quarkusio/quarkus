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

public class DialectSpecificSettingsMariaDBIgnoredTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-start-offline-mariadb-dialect.properties", "application.properties"))
            .setLogRecordPredicate(record -> HibernateProcessorUtil.class.getName().equals(record.getLoggerName()))
            .overrideConfigKey("quarkus.datasource.db-kind", "") // This will override to default which is H2
            .overrideConfigKey("quarkus.hibernate-orm.dialect.storage-engine", "")
            .overrideConfigKey("quarkus.hibernate-orm.dialect.mariadb.bytes-per-character", "8") // This will be ignored
            .overrideConfigKey("quarkus.hibernate-orm.dialect.mariadb.no-backslash-escapes", "true") // This will be ignored
            .setLogRecordPredicate(record -> HibernateProcessorUtil.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .anyMatch(
                                l -> l.contains("MariaDB specific settings being ignored because the database is not MariaDB"));
            });

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Test
    public void applicationStarts() {
        assertThat(entityManagerFactory.getProperties().get("hibernate.dialect.mysql.bytes_per_character"))
                .isEqualTo(null);
        assertThat(entityManagerFactory.getProperties().get("hibernate.dialect.mysql.no_backslash_escapes"))
                .isEqualTo(null);
    }
}

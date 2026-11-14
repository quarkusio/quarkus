package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorSupport;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test MySQL storage engine with H2
 */
public class StorageSpecificMysqlDBIgnoredTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-start-offline-mariadb-dialect.properties", "application.properties"))
            .setLogRecordPredicate(record -> HibernateProcessorSupport.class.getName().equals(record.getLoggerName()))
            .overrideConfigKey("quarkus.datasource.db-kind", "") // This will override to default which is H2
            .overrideConfigKey("quarkus.hibernate-orm.dialect.mysql.storage-engine", "innodb") // This will be ignored
            .setLogRecordPredicate(record -> HibernateProcessorSupport.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .anyMatch(l -> l.contains("The storage engine set through configuration property"));
                assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .anyMatch(l -> l.contains("is being ignored because the database is neither MySQL nor MariaDB."));
                // The properties file also sets the MariaDB storage engine, which is reported first
                assertThat(records)
                        .extracting(LOG_FORMATTER::formatMessage)
                        .anyMatch(l -> l.contains("'quarkus.hibernate-orm.dialect.mariadb.storage-engine'"));
            });

    @Test
    public void applicationStarts() {
        // Application starts successfully
    }
}

package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Formatter;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorSupport;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test MySQL storage engine with H2, without the MariaDB storage engine
 */
public class StorageSpecificMysqlDBOnlyIgnoredTest {

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    // Sets quarkus.hibernate-orm.dialect.mysql.storage-engine=innodb, which will be ignored
                    .addAsResource("application-start-offline-mysql-dialect.properties", "application.properties"))
            .overrideConfigKey("quarkus.datasource.db-kind", "") // This will override to default which is H2
            .setLogRecordPredicate(record -> HibernateProcessorSupport.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LOG_FORMATTER::formatMessage)
                    .anyMatch(l -> l.contains("'quarkus.hibernate-orm.dialect.mysql.storage-engine'")
                            && l.contains("is being ignored because the database is neither MySQL nor MariaDB.")));

    @Test
    public void applicationStarts() {
        // Application starts successfully
    }
}

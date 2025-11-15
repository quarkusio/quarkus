package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test MySQL storage engine with H2
 */
public class StorageSpecificMysqlDBIgnoredTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application-start-offline-mariadb-dialect.properties", "application.properties"))
            .setLogRecordPredicate(record -> HibernateProcessorUtil.class.getName().equals(record.getLoggerName()))
            .overrideConfigKey("quarkus.datasource.db-kind", "") // This will override to default which is H2
            .overrideConfigKey("quarkus.hibernate-orm.dialect.storage-engine", "")
            .overrideConfigKey("quarkus.hibernate-orm.dialect.mysql.storage-engine", "innodb") // This will be ignored
            .setLogRecordPredicate(record -> HibernateProcessorUtil.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .anyMatch(l -> l.contains("The storage engine set through configuration property"));
                assertThat(records)
                        .extracting(LogRecord::getMessage)
                        .anyMatch(l -> l.contains("is being ignored because the database is neither MySQL nor MariaDB."));
            });

    @Test
    public void applicationStarts() {
        // Application starts successfuly
    }
}

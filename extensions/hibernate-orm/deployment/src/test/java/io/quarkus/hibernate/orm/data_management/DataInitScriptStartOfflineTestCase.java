package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.runtime.schema.InitScriptSupport;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * When starting offline, the data init script (data.sql) is not executed by default,
 * and no warning is logged about it: the application must start without connecting to the database.
 */
public class DataInitScriptStartOfflineTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("data.sql")
                    .addAsResource("application-start-offline.properties", "application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && InitScriptSupport.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage).isEmpty());

    @Inject
    Session session;

    @Test
    public void applicationStarts() {
        assertThat(session).isNotNull();
    }
}

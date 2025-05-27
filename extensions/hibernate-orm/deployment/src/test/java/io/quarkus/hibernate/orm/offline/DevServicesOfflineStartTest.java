package io.quarkus.hibernate.orm.offline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.LogRecord;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DevServicesOfflineStartTest {

    // A simple runner like this will trigger Dev Services
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.hibernate-orm.database.start-offline", "true")
            .withEmptyApplication()
            .setLogRecordPredicate(record -> "io.quarkus.config".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                assertThat(records) // Configuration keys mispelled
                        .extracting(LogRecord::getMessage)
                        .noneMatch(msg -> msg.contains("Unrecognized configuration key"));
            });

    @Test
    public void testDevServices() {
        String value = ConfigProvider.getConfig()
                .getValue("quarkus.hibernate-orm.schema-management.strategy", String.class);
        assertThat(value).isEqualTo("none");
    }

}

package io.quarkus.hibernate.orm.offline;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DevServicesOfflineStartDisabledTest {

    // A simple runner like this will trigger Dev Services
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.hibernate-orm.database.start-offline", "false")
            .withEmptyApplication()
            .setLogRecordPredicate(record -> "io.quarkus.config".equals(record.getLoggerName()));

    @Test
    public void testOfflineDisabledStrategyDropCreate() {
        String value = ConfigProvider.getConfig()
                .getValue("quarkus.hibernate-orm.schema-management.strategy", String.class);
        assertThat(value).isEqualTo("drop-and-create");
    }

}

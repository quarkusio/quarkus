package io.quarkus.hibernate.reactive.mapping.timezone;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.SchemaUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

public class TimezoneDefaultStorageAutoTest extends AbstractTimezoneDefaultStorageTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EntityWithTimezones.class)
                    .addClasses(SchemaUtil.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.timezone.default-storage", "auto");

    @Test
    public void schema() {
        assertThat(SchemaUtil.getColumnNames(EntityWithTimezones.class, mappingMetamodel()))
                .contains("zonedDateTime_tz", "offsetDateTime_tz", "offsetTime_tz");

        assertThat(SchemaUtil.getColumnTypeName(EntityWithTimezones.class, "zonedDateTime", mappingMetamodel()))
                .isEqualTo("TIMESTAMP_UTC");
        assertThat(SchemaUtil.getColumnTypeName(EntityWithTimezones.class, "offsetDateTime", mappingMetamodel()))
                .isEqualTo("TIMESTAMP_UTC");
    }

    @Test
    @RunOnVertxContext
    public void persistAndLoad(UniAsserter asserter) {
        // Native storage is not supported with PostgreSQL, so we'll effectively use COLUMN.
        assertPersistedThenLoadedValues(asserter,
                // Column storage preserves the offset, but not the zone ID: https://hibernate.atlassian.net/browse/HHH-16289
                PERSISTED_ZONED_DATE_TIME.withZoneSameInstant(PERSISTED_ZONED_DATE_TIME.getOffset()),
                PERSISTED_OFFSET_DATE_TIME,
                PERSISTED_OFFSET_TIME);
    }
}

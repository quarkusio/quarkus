package io.quarkus.hibernate.reactive.mapping.timezone;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.SchemaUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

public class TimezoneDefaultStorageNormalizeTest extends AbstractTimezoneDefaultStorageTest {

    private static final ZoneId JDBC_TIMEZONE = ZoneId.of("America/Los_Angeles");

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EntityWithTimezones.class)
                    .addClasses(SchemaUtil.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.timezone.default-storage", "normalize")
            .overrideConfigKey("quarkus.hibernate-orm.jdbc.timezone", JDBC_TIMEZONE.getId());

    @Test
    public void schema() {
        assertThat(SchemaUtil.getColumnNames(EntityWithTimezones.class, mappingMetamodel()))
                .doesNotContain("zonedDateTime_tz", "offsetDateTime_tz", "offsetTime_tz");
        assertThat(SchemaUtil.getColumnTypeName(EntityWithTimezones.class, "zonedDateTime", mappingMetamodel()))
                .isEqualTo("TIMESTAMP");
        assertThat(SchemaUtil.getColumnTypeName(EntityWithTimezones.class, "offsetDateTime", mappingMetamodel()))
                .isEqualTo("TIMESTAMP");
    }

    @Test
    @RunOnVertxContext
    public void persistAndLoad(UniAsserter asserter) {
        assertPersistedThenLoadedValues(asserter,
                PERSISTED_ZONED_DATE_TIME.withZoneSameInstant(ZoneId.systemDefault()),
                PERSISTED_OFFSET_DATE_TIME.withOffsetSameInstant(
                        ZoneId.systemDefault().getRules().getOffset(PERSISTED_OFFSET_DATE_TIME.toInstant())),
                PERSISTED_OFFSET_TIME.withOffsetSameInstant(
                        ZoneId.systemDefault().getRules().getOffset(Instant.now())));
    }
}

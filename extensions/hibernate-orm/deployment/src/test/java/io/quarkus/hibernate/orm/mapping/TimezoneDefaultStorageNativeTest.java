package io.quarkus.hibernate.orm.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class TimezoneDefaultStorageNativeTest extends AbstractTimezoneDefaultStorageTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EntityWithTimezones.class)
                    .addClasses(SchemaUtil.class, SmokeTestUtils.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.timezone.default-storage", "native");

    @Test
    public void schema() throws Exception {
        assertThat(SchemaUtil.getColumnNames(sessionFactory, EntityWithTimezones.class))
                .doesNotContain("zonedDateTime_tz", "offsetDateTime_tz");
        assertThat(SchemaUtil.getColumnTypeName(sessionFactory, EntityWithTimezones.class, "zonedDateTime"))
                .isEqualTo("TIMESTAMP_WITH_TIMEZONE");
        assertThat(SchemaUtil.getColumnTypeName(sessionFactory, EntityWithTimezones.class, "offsetDateTime"))
                .isEqualTo("TIMESTAMP_WITH_TIMEZONE");
    }

    @Test
    public void persistAndLoad() {
        long id = persistWithValuesToTest();
        // For some reason native storage (with H2 at least) preserves the offset, but not the zone ID.
        assertLoadedValues(id, PERSISTED_ZONED_DATE_TIME.withZoneSameInstant(PERSISTED_ZONED_DATE_TIME.getOffset()),
                PERSISTED_OFFSET_DATE_TIME);
    }
}

package io.quarkus.hibernate.orm.mapping.timezone;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class TimezoneDefaultStorageNormalizeUtcTest extends AbstractTimezoneDefaultStorageTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(EntityWithTimezones.class).addClasses(SchemaUtil.class,
                    SmokeTestUtils.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.timezone.default-storage", "normalize-utc");

    @Test
    public void schema() throws Exception {
        assertThat(SchemaUtil.getColumnNames(sessionFactory, EntityWithTimezones.class))
                .doesNotContain("zonedDateTime_tz", "offsetDateTime_tz", "offsetTime_tz");
        assertThat(SchemaUtil.getColumnTypeName(sessionFactory, EntityWithTimezones.class, "zonedDateTime"))
                .isEqualTo("TIMESTAMP_UTC");
        assertThat(SchemaUtil.getColumnTypeName(sessionFactory, EntityWithTimezones.class, "offsetDateTime"))
                .isEqualTo("TIMESTAMP_UTC");
    }

    @Test
    public void persistAndLoad() {
        long id = persistWithValuesToTest();
        assertLoadedValues(id, PERSISTED_ZONED_DATE_TIME.withZoneSameInstant(ZoneOffset.UTC),
                PERSISTED_OFFSET_DATE_TIME.withOffsetSameInstant(ZoneOffset.UTC),
                PERSISTED_OFFSET_TIME.withOffsetSameInstant(ZoneOffset.UTC));
    }
}

package io.quarkus.hibernate.orm.mapping.timezone;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class TimezoneDefaultStorageNormalizeTest extends AbstractTimezoneDefaultStorageTest {

    private static final ZoneId JDBC_TIMEZONE = ZoneId.of("America/Los_Angeles");

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(EntityWithTimezones.class).addClasses(SchemaUtil.class,
                    SmokeTestUtils.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.timezone.default-storage", "normalize")
            .overrideConfigKey("quarkus.hibernate-orm.jdbc.timezone", JDBC_TIMEZONE.getId());

    @Test
    public void schema() throws Exception {
        assertThat(SchemaUtil.getColumnNames(sessionFactory, EntityWithTimezones.class))
                .doesNotContain("zonedDateTime_tz", "offsetDateTime_tz", "offsetTime_tz");
        assertThat(SchemaUtil.getColumnTypeName(sessionFactory, EntityWithTimezones.class, "zonedDateTime"))
                .isEqualTo("TIMESTAMP");
        assertThat(SchemaUtil.getColumnTypeName(sessionFactory, EntityWithTimezones.class, "offsetDateTime"))
                .isEqualTo("TIMESTAMP");
    }

    @Test
    public void persistAndLoad() {
        long id = persistWithValuesToTest();
        assertLoadedValues(id, PERSISTED_ZONED_DATE_TIME.withZoneSameInstant(ZoneId.systemDefault()),
                PERSISTED_OFFSET_DATE_TIME.withOffsetSameInstant(
                        ZoneId.systemDefault().getRules().getOffset(PERSISTED_OFFSET_DATE_TIME.toInstant())),
                PERSISTED_OFFSET_TIME
                        .withOffsetSameInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now())));
    }
}

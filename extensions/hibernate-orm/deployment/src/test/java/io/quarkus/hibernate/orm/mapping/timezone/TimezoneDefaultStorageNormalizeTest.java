package io.quarkus.hibernate.orm.mapping.timezone;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusExtensionTest;

public class TimezoneDefaultStorageNormalizeTest extends AbstractTimezoneDefaultStorageTest {

    private static final ZoneId JDBC_TIMEZONE = ZoneId.of("America/Los_Angeles");

    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EntityWithTimezones.class)
                    .addClasses(SchemaUtil.class, SmokeTestUtils.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.timezone.default-storage", "normalize")
            .overrideConfigKey("quarkus.hibernate-orm.jdbc.timezone", JDBC_TIMEZONE.getId());

    @Test
    public void schema() throws Exception {
        assertThat(SchemaUtil.getColumnNames(sessionFactory, EntityWithTimezones.class))
                .doesNotContain("zonedDateTime_tz", "offsetDateTime_tz", "offsetTime_tz");
        assertDirectJdbcAccess();
    }

    @Test
    public void persistAndLoad() {
        long id = persistWithValuesToTest();
        assertLoadedValues(id,
                // Direct JDBC access retains the offsets even when a JDBC timezone is configured.
                PERSISTED_ZONED_DATE_TIME.withZoneSameInstant(PERSISTED_ZONED_DATE_TIME.getOffset()),
                PERSISTED_OFFSET_DATE_TIME,
                PERSISTED_OFFSET_TIME);
    }
}

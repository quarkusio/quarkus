package io.quarkus.hibernate.orm.mapping.timezone;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusExtensionTest;

@ParameterizedClass
@MethodSource("directJdbcModes")
public class TimezoneDefaultStorageNormalizeTest extends AbstractTimezoneDefaultStorageTest {

    private static final ZoneId JDBC_TIMEZONE = ZoneId.of("America/Los_Angeles");

    @Parameter
    public boolean useDirectJdbc;

    public static List<Arguments> directJdbcModes() {
        return List.of(Arguments.of(false), Arguments.of(true));
    }

    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EntityWithTimezones.class)
                    .addClasses(SchemaUtil.class, SmokeTestUtils.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.timezone.default-storage", "normalize")
            .overrideConfigKey("quarkus.hibernate-orm.jdbc.timezone", JDBC_TIMEZONE.getId());

    @BeforeParameterizedClassInvocation
    static void configure(boolean useDirectJdbc) {
        TEST.resetForParameterizedClass()
                .withApplicationRoot((jar) -> jar
                        .addClasses(EntityWithTimezones.class)
                        .addClasses(SchemaUtil.class, SmokeTestUtils.class))
                .withConfigurationResource("application.properties")
                .overrideConfigKey("quarkus.hibernate-orm.mapping.timezone.default-storage", "normalize")
                .overrideConfigKey("quarkus.hibernate-orm.jdbc.timezone", JDBC_TIMEZONE.getId())
                .overrideConfigKey("quarkus.hibernate-orm.mapping.java-time.use-direct-jdbc",
                        String.valueOf(useDirectJdbc));
    }

    @Test
    public void schema() throws Exception {
        assertThat(SchemaUtil.getColumnNames(sessionFactory, EntityWithTimezones.class))
                .doesNotContain("zonedDateTime_tz", "offsetDateTime_tz", "offsetTime_tz");
        if (useDirectJdbc) {
            assertDirectJdbcAccess();
        } else {
            assertThat(SchemaUtil.getColumnTypeName(sessionFactory, EntityWithTimezones.class, "zonedDateTime"))
                    .isEqualTo("TIMESTAMP");
            assertThat(SchemaUtil.getColumnTypeName(sessionFactory, EntityWithTimezones.class, "offsetDateTime"))
                    .isEqualTo("TIMESTAMP");
        }
    }

    @Test
    public void persistAndLoad() {
        long id = persistWithValuesToTest();
        if (useDirectJdbc) {
            // Direct JDBC access retains the offsets even when a JDBC timezone is configured.
            assertLoadedValues(id,
                    PERSISTED_ZONED_DATE_TIME.withZoneSameInstant(PERSISTED_ZONED_DATE_TIME.getOffset()),
                    PERSISTED_OFFSET_DATE_TIME,
                    PERSISTED_OFFSET_TIME);
        } else {
            // Legacy mode: values are normalized to the JVM timezone.
            assertLoadedValues(id,
                    PERSISTED_ZONED_DATE_TIME.withZoneSameInstant(ZoneId.systemDefault()),
                    PERSISTED_OFFSET_DATE_TIME.withOffsetSameInstant(
                            ZoneId.systemDefault().getRules().getOffset(PERSISTED_OFFSET_DATE_TIME.toInstant())),
                    PERSISTED_OFFSET_TIME.withOffsetSameInstant(
                            ZoneId.systemDefault().getRules().getOffset(Instant.now())));
        }
    }
}

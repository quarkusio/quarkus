package io.quarkus.hibernate.orm.mapping.timezone;

import static org.assertj.core.api.Assertions.assertThat;

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
public class TimezoneDefaultStorageNativeTest extends AbstractTimezoneDefaultStorageTest {

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
            .overrideConfigKey("quarkus.hibernate-orm.mapping.timezone.default-storage", "native");

    @BeforeParameterizedClassInvocation
    static void configure(boolean useDirectJdbc) {
        TEST.resetForParameterizedClass()
                .withApplicationRoot((jar) -> jar
                        .addClasses(EntityWithTimezones.class)
                        .addClasses(SchemaUtil.class, SmokeTestUtils.class))
                .withConfigurationResource("application.properties")
                .overrideConfigKey("quarkus.hibernate-orm.mapping.timezone.default-storage", "native")
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
                    .isEqualTo("TIMESTAMP_WITH_TIMEZONE");
            assertThat(SchemaUtil.getColumnTypeName(sessionFactory, EntityWithTimezones.class, "offsetDateTime"))
                    .isEqualTo("TIMESTAMP_WITH_TIMEZONE");
        }
    }

    @Test
    public void persistAndLoad() {
        long id = persistWithValuesToTest();
        // Native storage preserves the offset, but not the zone ID: https://hibernate.atlassian.net/browse/HHH-16289
        assertLoadedValues(id,
                PERSISTED_ZONED_DATE_TIME.withZoneSameInstant(PERSISTED_ZONED_DATE_TIME.getOffset()),
                PERSISTED_OFFSET_DATE_TIME,
                PERSISTED_OFFSET_TIME);
    }
}

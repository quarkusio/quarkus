package io.quarkus.hibernate.orm.formatmapper;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class FormatMapperBehaviorJsonObjectMapperAdjustedPropertiesTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyJsonEntity.class)
                    .addClasses(SchemaUtil.class, SmokeTestUtils.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.jackson.write-dates-as-timestamps", "true") // to keep Jackson's defaults
            .overrideConfigKey("quarkus.jackson.write-durations-as-timestamps", "true") // to keep Jackson's defaults
            .overrideConfigKey("quarkus.jackson.accept-case-insensitive-enums", "false") // to keep Jackson's defaults
    // .overrideConfigKey( "quarkus.jackson.timezone", "UTC" ) // to keep Jackson's defaults (this is already Quarkus' default)
    ;

    @Inject
    SessionFactory sessionFactory;

    @Test
    void smoke() {
        // We really just care ot see if the SF is built successfully here or not;
        assertThat(SchemaUtil.getColumnNames(sessionFactory, MyJsonEntity.class))
                .contains("properties", "amount1", "amount2")
                .doesNotContain("amountDifference");
    }
}

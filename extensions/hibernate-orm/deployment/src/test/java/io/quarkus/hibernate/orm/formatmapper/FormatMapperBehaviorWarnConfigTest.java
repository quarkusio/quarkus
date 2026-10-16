package io.quarkus.hibernate.orm.formatmapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class FormatMapperBehaviorWarnConfigTest {
    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyJsonEntity.class)
                    .addClasses(SchemaUtil.class, SmokeTestUtils.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.format.global", "warn")
            .assertException(t -> assertThat(t)
                    .rootCause()
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("quarkus.hibernate-orm.mapping.format.global"));

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }
}

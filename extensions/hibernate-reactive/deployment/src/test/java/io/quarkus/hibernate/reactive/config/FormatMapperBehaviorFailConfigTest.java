package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class FormatMapperBehaviorFailConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.mapping.format.global", "fail")
            .assertException(t -> assertThat(t)
                    .rootCause()
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("quarkus.hibernate-orm.mapping.format.global"));

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }
}

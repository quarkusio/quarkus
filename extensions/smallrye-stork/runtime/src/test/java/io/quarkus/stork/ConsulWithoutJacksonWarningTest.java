package io.quarkus.stork;

import static org.assertj.core.api.Fail.fail;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class ConsulWithoutJacksonWarningTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setForcedDependencies(
                    Arrays.asList(
                            Dependency.of("io.quarkus", "quarkus-smallrye-stork", Version.getVersion()),
                            Dependency.of("io.smallrye.stork", "stork-service-registration-consul", "2.7.9")))
            .assertException(throwable -> Assertions.assertThat(throwable)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("quarkus-jackson"));

    @Test
    public void testExceptionIsThrownWhenConsulPresentWithoutJackson() {
        fail("Should fail");
    }
}
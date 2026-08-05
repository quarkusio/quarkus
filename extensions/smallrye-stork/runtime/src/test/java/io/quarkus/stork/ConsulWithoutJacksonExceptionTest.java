package io.quarkus.stork;

import static org.assertj.core.api.Fail.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Properties;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class ConsulWithoutJacksonExceptionTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setForcedDependencies(
                    Arrays.asList(
                            Dependency.of("io.quarkus", "quarkus-smallrye-stork", Version.getVersion()),
                            Dependency.of("io.smallrye.stork", "stork-service-registration-consul", storkVersion())))
            .assertException(throwable -> Assertions.assertThat(throwable)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("quarkus-jackson"));

    @Test
    public void testExceptionIsThrownWhenConsulPresentWithoutJackson() {
        fail("Should fail");
    }

    private static String storkVersion() {
        try (InputStream is = ConsulWithoutJacksonExceptionTest.class
                .getResourceAsStream("/META-INF/maven/io.smallrye.stork/stork-core/pom.properties")) {
            Properties props = new Properties();
            props.load(is);
            return props.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
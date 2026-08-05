package io.quarkus.stork;

import static org.assertj.core.api.Fail.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

public class MultipleRegistrarsWithoutTypeConfigRegistrationTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.INFO.intValue())
            .setForcedDependencies(
                    Arrays.asList(
                            Dependency.of("io.quarkus", "quarkus-smallrye-stork", Version.getVersion()),
                            Dependency.of("io.smallrye.stork", "stork-service-registration-static-list", storkVersion())))
            .overrideConfigKey("quarkus.stork.red-service.service-registrar.ip-address", "145.123.145.122")
            .overrideConfigKey("quarkus.stork.blue-service.service-registrar.type", "static")
            .overrideConfigKey("quarkus.stork.blue-service.service-registrar.ip-address", "145.123.145.157")
            .assertException(throwable -> {
                Assertions.assertThat(throwable)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage(
                                "Impossible to register service. Missing required 'type' for the following services: red-service");
            });

    @Test
    public void testBuildShouldFail() {
        fail("Should fail");
    }

    private static String storkVersion() {
        try (InputStream is = MultipleRegistrarsWithoutTypeConfigRegistrationTest.class
                .getResourceAsStream("/META-INF/maven/io.smallrye.stork/stork-core/pom.properties")) {
            Properties props = new Properties();
            props.load(is);
            return props.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

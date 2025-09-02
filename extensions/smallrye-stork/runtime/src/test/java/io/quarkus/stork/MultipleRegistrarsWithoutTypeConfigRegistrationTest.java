package io.quarkus.stork;

import static org.assertj.core.api.Fail.fail;

import java.util.Arrays;
import java.util.logging.Level;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleRegistrarsWithoutTypeConfigRegistrationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.INFO.intValue())
            .setForcedDependencies(
                    Arrays.asList(
                            Dependency.of("io.quarkus", "quarkus-smallrye-stork", Version.getVersion()),
                            Dependency.of("io.smallrye.stork", "stork-service-registration-consul", "2.7.6")))
            .overrideConfigKey("quarkus.stork.red-service.service-registrar.ip-address", "145.123.145.122")
            .overrideConfigKey("quarkus.stork.blue-service.service-registrar.type", "consul")
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
}

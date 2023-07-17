package io.quarkus.opentelemetry.deployment;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusProdModeTest;

public class TracerWithInvalidExtensionTest {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withEmptyApplication()
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-smallrye-opentracing", Version.getVersion())))
            .setExpectedException(ConfigurationException.class);

    @Test
    void failStart() {
        Assertions.fail("Test should not be run as deployment should fail");
    }
}

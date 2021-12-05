package io.quarkus.opentelemetry.deployment;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusProdModeTest;

public class TracerWithInvalidExtensionTest {
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withEmptyApplication()
            .setForcedDependencies(
                    Collections.singletonList(
                            new AppArtifact("io.quarkus", "quarkus-smallrye-opentracing", "999-SNAPSHOT")))
            .setExpectedException(ConfigurationException.class);

    @Test
    void failStart() {
        Assertions.fail("Test should not be run as deployment should fail");
    }
}

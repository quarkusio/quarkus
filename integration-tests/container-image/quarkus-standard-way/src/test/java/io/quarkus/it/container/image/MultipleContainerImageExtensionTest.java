package io.quarkus.it.container.image;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.QuarkusProdModeTest;

public class MultipleContainerImageExtensionTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("multiple-container-image")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectedException(IllegalStateException.class)
            .setForcedDependencies(
                    Arrays.asList(
                            new AppArtifact("io.quarkus", "quarkus-container-image-jib", Version.getVersion()),
                            new AppArtifact("io.quarkus", "quarkus-container-image-docker", Version.getVersion())));

    @Test
    public void testBuildShouldFail() {
        fail("Build should have failed and therefore this method should not have been called");
    }
}

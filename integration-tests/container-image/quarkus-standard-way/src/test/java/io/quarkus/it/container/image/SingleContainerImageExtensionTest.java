package io.quarkus.it.container.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class SingleContainerImageExtensionTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("single-container-image")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setForcedDependencies(
                    Collections.singletonList(
                            new AppArtifact("io.quarkus", "quarkus-container-image-jib", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testBuildShouldSucceed() {
        assertThat(prodModeTestResults.getResults()).hasSize(1);
    }
}

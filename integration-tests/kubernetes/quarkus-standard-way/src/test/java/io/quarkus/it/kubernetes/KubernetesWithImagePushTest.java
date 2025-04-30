package io.quarkus.it.kubernetes;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithImagePushTest extends BaseWithRemoteRegistry {

    private static final String APP_NAME = "kubernetes-with-remote-image-push";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.container-image.group", "user")
            .overrideConfigKey("quarkus.container-image.image", "quay.io/user/" + APP_NAME + ":1.0")
            .overrideConfigKey("quarkus.container-image.username", "me")
            .overrideConfigKey("quarkus.container-image.password", "pass")
            .overrideConfigKey("quarkus.kubernetes.generate-image-pull-secret", "true")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-container-image-docker", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        assertGeneratedResources(APP_NAME, "kubernetes", prodModeTestResults.getBuildDir());
    }
}

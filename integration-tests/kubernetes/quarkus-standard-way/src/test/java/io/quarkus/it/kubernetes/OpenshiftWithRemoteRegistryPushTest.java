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

public class OpenshiftWithRemoteRegistryPushTest extends BaseOpenshiftWithRemoteRegistry {

    private static final String APP_NAME = "openshift-with-remote-docker-registry-push";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.container-image.group", "user")
            .overrideConfigKey("quarkus.container-image.registry", "quay.io")
            .overrideConfigKey("quarkus.container-image.username", "me")
            .overrideConfigKey("quarkus.container-image.password", "pass")
            .overrideConfigKey("quarkus.openshift.generate-image-pull-secret", "true")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        assertGeneratedResources(APP_NAME, "0.1-SNAPSHOT", prodModeTestResults.getBuildDir());
    }
}

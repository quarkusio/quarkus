package io.quarkus.it.kubernetes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithFlywayIntAndCustomServiceAccountTest extends KubernetesWithFlywayInitBase {

    private static final String NAME = "kubernetes-with-flyway";
    private static final String IMAGE_PULL_SECRET = "my-pull-secret";
    private static final String SERVICE_ACCOUNT = "my-service-account";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .setLogFileName("k8s.log")
            .overrideConfigKey("quarkus.kubernetes.image-pull-secrets", IMAGE_PULL_SECRET)
            .overrideConfigKey("quarkus.kubernetes.service-account", SERVICE_ACCOUNT)
            .setForcedDependencies(Arrays.asList(
                    new AppArtifact("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    new AppArtifact("io.quarkus", "quarkus-flyway", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertGeneratedResources(kubernetesDir, NAME, IMAGE_PULL_SECRET, SERVICE_ACCOUNT);
    }
}

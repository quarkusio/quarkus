
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithJobResourceWhenUsingPicocliAndDeploymentTest {

    static final String APP_NAME = "kubernetes-with-job-resource-when-using-picocli-and-deployment";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-picocli", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).filteredOn(i -> i instanceof Deployment).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Deployment.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo(APP_NAME);
                });
            });
        });
    }
}

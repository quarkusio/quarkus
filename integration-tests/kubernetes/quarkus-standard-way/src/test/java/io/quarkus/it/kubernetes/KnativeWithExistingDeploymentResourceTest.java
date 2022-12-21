
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeWithExistingDeploymentResourceTest {

    private static final String APP_NAME = "knative-with-existing-deployment-resource";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties")
            .addCustomResourceEntry(Path.of("src", "main", "kubernetes", "knative.yml"),
                    "manifests/" + APP_NAME + "/knative.yml")
            .addBuildChainCustomizerEntries(
                    new QuarkusProdModeTest.BuildChainCustomizerEntry(
                            KubernetesWithCustomResourcesTest.CustomProjectRootBuildItemProducerProdMode.class,
                            Collections.singletonList(CustomProjectRootBuildItem.class), Collections.emptyList()));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.yml"));

        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("knative.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Deployment".equals(i.getKind())
                && "example".equals(i.getMetadata().getName()))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e).isInstanceOfSatisfying(Deployment.class, deployment -> {
                        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers()).allSatisfy(container -> {
                            assertThat(container.getLivenessProbe().getHttpGet().getPort().getIntVal()).isEqualTo(8080);
                            assertThat(container.getReadinessProbe().getHttpGet().getPort().getIntVal()).isEqualTo(8080);

                            assertThat(container.getResources().getRequests().get("memory").getAmount()).isEqualTo("128");
                            assertThat(container.getResources().getLimits().get("memory").getAmount()).isEqualTo("768");
                        });
                    });
                });
    }
}

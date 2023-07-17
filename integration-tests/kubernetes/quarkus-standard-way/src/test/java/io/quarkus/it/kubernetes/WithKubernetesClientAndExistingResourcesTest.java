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
import io.quarkus.builder.Version;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class WithKubernetesClientAndExistingResourcesTest {
    private static final String APPLICATION_NAME = "client-existing-resources";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APPLICATION_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-" + APPLICATION_NAME + ".properties")
            .addCustomResourceEntry(Path.of("src", "main", "kubernetes", "kubernetes.yml"),
                    "manifests/kubernetes-with-" + APPLICATION_NAME + "/kubernetes.yml")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes-client", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())))
            .addBuildChainCustomizerEntries(
                    new QuarkusProdModeTest.BuildChainCustomizerEntry(
                            KubernetesWithCustomResourcesTest.CustomProjectRootBuildItemProducerProdMode.class,
                            Collections.singletonList(CustomProjectRootBuildItem.class), Collections.emptyList()));

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

        assertThat(kubernetesList).filteredOn(h -> "Deployment".equals(h.getKind())).allSatisfy(h -> {
            Deployment deployment = (Deployment) h;
            String serviceAccountName = deployment.getSpec().getTemplate().getSpec().getServiceAccountName();
            if (h.getMetadata().getName().equals(APPLICATION_NAME)) {
                assertThat(serviceAccountName).isEqualTo(APPLICATION_NAME);
            } else {
                assertThat(serviceAccountName).isNull();
            }
        });

        assertThat(kubernetesList).filteredOn(h -> "ServiceAccount".equals(h.getKind())).singleElement()
                .satisfies(h -> assertThat(h.getMetadata().getName()).isEqualTo(APPLICATION_NAME));

        assertThat(kubernetesList).filteredOn(h -> "RoleBinding".equals(h.getKind())).singleElement()
                .satisfies(h -> assertThat(h.getMetadata().getName()).isEqualTo(APPLICATION_NAME + "-view"));

        // check that if quarkus.kubernetes.namespace is set, "manually" set namespaces are not overwritten
        assertThat(kubernetesList).filteredOn(h -> "ConfigMap".equals(h.getKind())).singleElement().satisfies(h -> {
            final var metadata = h.getMetadata();
            assertThat(metadata.getName()).isEqualTo("foo");
            assertThat(metadata.getNamespace()).isEqualTo("foo");
        });
    }
}

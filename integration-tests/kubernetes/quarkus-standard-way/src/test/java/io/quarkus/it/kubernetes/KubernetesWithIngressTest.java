
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
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithIngressTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("kubernetes-with-ingress")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-ingress.properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(
                    Collections.singletonList(new AppArtifact("io.quarkus", "quarkus-kubernetes", Version.getVersion())));

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

        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("kubernetes-with-ingress");
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {

                    });
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> "Ingress".equals(i.getKind())).singleElement().satisfies(item -> {
            assertThat(item).isInstanceOfSatisfying(Ingress.class, ingress -> {
                //Check that labels and annotations are also applied to Routes (#10260)
                assertThat(ingress.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("kubernetes-with-ingress");
                });

                assertThat(ingress.getSpec().getRules()).allSatisfy(rule -> {
                    assertThat(rule.getHttp().getPaths()).allSatisfy(path -> {
                        assertThat(path.getPath()).isNotBlank();
                        assertThat(path.getPathType()).isNotBlank();
                    });
                });
            });
        });
    }
}

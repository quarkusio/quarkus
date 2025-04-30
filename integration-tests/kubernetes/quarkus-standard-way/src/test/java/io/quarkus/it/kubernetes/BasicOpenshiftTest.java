package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class BasicOpenshiftTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("basic-openshift")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("openshift.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(2));
        List<HasMetadata> openshiftList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList).filteredOn(h -> "Deployment".equals(h.getKind())).singleElement().satisfies(h -> {
            Deployment deployment = (Deployment) h;

            // metadata assertions
            assertThat(deployment.getMetadata().getName()).isEqualTo("basic-openshift");
            assertThat(deployment.getMetadata().getLabels().get("app.openshift.io/runtime")).isEqualTo("quarkus");
            assertThat(deployment.getMetadata().getLabels().get("app.kubernetes.io/managed-by")).isEqualTo("quarkus");
            assertThat(deployment.getMetadata().getNamespace()).isNull();

            // spec assertions
            assertThat(deployment.getSpec().getReplicas()).isEqualTo(1);
            assertThat(deployment.getSpec().getSelector().getMatchLabels()).containsOnly(
                    entry("app.kubernetes.io/name", "basic-openshift"),
                    entry("app.kubernetes.io/version", "0.1-SNAPSHOT"));

            // containers assertions
            assertThat(deployment.getSpec().getTemplate().getSpec().getContainers()).satisfiesExactly(container -> {
                assertThat(container.getImagePullPolicy()).isEqualTo("Always"); // expect the default value
            });
        });

        assertThat(openshiftList).filteredOn(h -> "Service".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getNamespace()).isNull();
                });

                assertThat(s.getSpec()).satisfies(spec -> {
                    assertThat(spec.getSelector()).containsOnly(entry("app.kubernetes.io/name", "basic-openshift"),
                            entry("app.kubernetes.io/version", "0.1-SNAPSHOT"));
                });
            });
        });
    }
}

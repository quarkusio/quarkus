package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicy;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithSpecifiedContainerNameTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("kubernetes-with-specified-container-name")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-specified-container-name.properties")
            .overrideConfigKey("quarkus.openshift.deployment-kind", "deployment-config")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-container-image-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));

        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));
        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("foo");
                assertThat(m.getLabels()).contains(entry("app.kubernetes.io/name", "foo"));
            });

            assertThat(d.getSpec().getTemplate().getSpec().getContainers().get(0))
                    .satisfies(c -> assertThat(c.getName()).isEqualTo("bar"));
            assertThat(d.getSpec().getTemplate().getSpec().getContainers().get(0))
                    .satisfies(c -> assertThat(c.getCommand()).contains("my-command"));
            assertThat(d.getSpec().getTemplate().getSpec().getContainers().get(0))
                    .satisfies(c -> assertThat(c.getArgs()).containsExactly("A", "B"));
        });

        List<HasMetadata> openshiftList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("openshift.yml"));
        assertThat(openshiftList).filteredOn(h -> "DeploymentConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            AbstractObjectAssert<?, ?> specAssert = assertThat(h).extracting("spec");
            specAssert.extracting("template").extracting("spec").isInstanceOfSatisfying(PodSpec.class,
                    podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getCommand())
                                    .contains("my-openshift-command");
                            assertThat(container.getArgs()).containsExactly("C", "D");
                            assertThat(container)
                                    .satisfies(c -> assertThat(c.getName()).isEqualTo("obar"));
                        });
                    });

            specAssert.extracting("triggers").isInstanceOfSatisfying(Collection.class, c -> {
                assertThat(c).singleElement().satisfies(trigger -> {
                    assertThat(((DeploymentTriggerPolicy) trigger).getImageChangeParams().getContainerNames())
                            .contains("obar");
                });
            });

            assertThat(h.getMetadata().getName()).isIn("ofoo", "foo", "openjdk-17");
            assertThat(h.getMetadata().getLabels()).contains(entry("app.kubernetes.io/name", "ofoo"));
        });
    }
}

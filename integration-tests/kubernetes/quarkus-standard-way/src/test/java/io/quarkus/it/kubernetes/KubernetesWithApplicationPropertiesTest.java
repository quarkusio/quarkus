package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithApplicationPropertiesTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("kubernetes-with-application-properties")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-application.properties");

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

        assertThat(kubernetesList).hasSize(3);

        assertThat(kubernetesList).filteredOn(i -> "Deployment".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Deployment.class, d -> {
                assertThat(d.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("test-it");
                    assertThat(m.getLabels()).contains(entry("foo", "bar"));
                    assertThat(m.getNamespace()).isEqualTo("applications");
                });

                assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                    // ensure that the version is not part of the selector labels
                    assertThat(deploymentSpec.getSelector()).isNotNull().satisfies(labelSelector -> {
                        assertThat(labelSelector.getMatchLabels()).containsOnly(entry("app.kubernetes.io/name", "test-it"));
                    });
                    assertThat(deploymentSpec.getReplicas()).isEqualTo(3);
                    assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                        assertThat(t.getSpec()).satisfies(podSpec -> {
                            assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                                assertThat(container.getEnv()).extracting("name", "value")
                                        .contains(tuple("MY_ENV_VAR", "SOMEVALUE"));

                                assertThat(container.getEnv()).extracting("name", "valueFrom")
                                        .contains(tuple("MY_NAME",
                                                new EnvVarSourceBuilder().withNewFieldRef().withFieldPath("metadata.name")
                                                        .endFieldRef().build()));

                                assertThat(container.getImage())
                                        .isEqualTo("quay.io/grp/kubernetes-with-application-properties:0.1-SNAPSHOT");
                                assertThat(container.getPorts()).singleElement().satisfies(p -> {
                                    assertThat(p.getContainerPort()).isEqualTo(9090);
                                });
                                assertThat(container.getImagePullPolicy()).isEqualTo("IfNotPresent");
                            });
                        });
                    });
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> "Service".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getNamespace()).isEqualTo("applications");
                    assertThat(m.getLabels()).contains(entry("foo", "bar"));
                });

                assertThat(s.getSpec()).satisfies(spec -> {
                    assertEquals("NodePort", spec.getType());
                    assertThat(spec.getSelector()).containsOnly(entry("app.kubernetes.io/name", "test-it"));
                    assertThat(spec.getPorts()).hasSize(1).singleElement().satisfies(p -> {
                        assertThat(p.getPort()).isEqualTo(80);
                        assertThat(p.getTargetPort().getIntVal()).isEqualTo(9090);
                    });
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> "Ingress".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Ingress.class, in -> {
                //Check that labels and annotations are also applied to Ingresses (#10260)
                assertThat(i.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("test-it");
                    assertThat(m.getLabels()).contains(entry("foo", "bar"));
                    assertThat(m.getAnnotations()).contains(entry("bar", "baz"));
                    assertThat(m.getNamespace()).isEqualTo("applications");
                });

                assertThat(in.getSpec().getRules()).singleElement().satisfies(r -> {
                    assertThat(r.getHost()).isEqualTo("example.com");
                });
            });
        });
    }

}

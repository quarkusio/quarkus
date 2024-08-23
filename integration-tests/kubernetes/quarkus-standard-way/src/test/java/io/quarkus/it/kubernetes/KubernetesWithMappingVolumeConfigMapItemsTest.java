package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithMappingVolumeConfigMapItemsTest {
    private static final String APPLICATION_NAME = "mapping-volume-configmap-items";
    private static final String VOLUME_NAME = "volume-config";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APPLICATION_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-" + APPLICATION_NAME + ".properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void ensureVolumeIsCorrectlyMappedWithItems() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));
        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(APPLICATION_NAME);
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getVolumeMounts()).singleElement().satisfies(volumeMount -> {
                                assertThat(volumeMount.getName()).isEqualTo(VOLUME_NAME);
                                assertThat(volumeMount.getMountPath()).isEqualTo("/etc/config");
                            });
                        });

                        assertThat(podSpec.getVolumes()).singleElement().satisfies(volume -> {
                            assertThat(volume.getName()).isEqualTo(VOLUME_NAME);
                            assertThat(volume.getConfigMap()).isNotNull();
                            assertThat(volume.getConfigMap().getName()).isEqualTo("the-config-map-name");
                            assertThat(volume.getConfigMap().getItems()).singleElement().satisfies(keyToPath -> {
                                assertThat(keyToPath.getKey()).isEqualTo("key");
                                assertThat(keyToPath.getPath()).isEqualTo("/from/path");
                            });
                        });
                    });
                });
            });
        });
    }
}

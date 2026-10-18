package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.RevisionSpec;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeWithAppConfigMapTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("knative-with-app-config-map")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("knative-with-app-config-map.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(2));

        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("knative.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Service".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getSpec()).satisfies(spec -> {
                    assertThat(s.getMetadata()).satisfies(m -> {
                        assertThat(m.getName()).isEqualTo("knative-with-app-config-map");
                    });
                });
            });

            AbstractObjectAssert<?, ?> specAssert = assertThat(i).extracting("spec");
            specAssert.extracting("template").extracting("spec").isInstanceOfSatisfying(RevisionSpec.class,
                    revisionSpec -> {
                        assertThat(revisionSpec.getContainers()).singleElement().satisfies(container -> {
                            List<EnvVar> envVars = container.getEnv();
                            assertThat(envVars).anySatisfy(envVar -> {
                                assertThat(envVar.getName()).isEqualTo("SMALLRYE_CONFIG_LOCATIONS");
                                assertThat(envVar.getValue()).isEqualTo("/mnt/app-config-map");
                            });

                            List<VolumeMount> mounts = container.getVolumeMounts();
                            assertThat(mounts).anySatisfy(mount -> {
                                assertThat(mount.getName()).isEqualTo("app-config-map");
                                assertThat(mount.getMountPath()).isEqualTo("/mnt/app-config-map");
                            });
                        });
                        List<Volume> volumes = revisionSpec.getVolumes();
                        assertThat(volumes).anySatisfy(volume -> {
                            assertThat(volume.getName()).isEqualTo("app-config-map");
                            assertThat(volume.getConfigMap().getName()).isEqualTo("my-kn-config-map");
                        });
                    });
        });
    }
}

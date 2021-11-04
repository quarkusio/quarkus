package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithAppSecretTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift-with-app-secret")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("openshift-with-app-secret.properties")
            .setForcedDependencies(Collections.singletonList(
                    new AppArtifact("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");

        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> openshiftList = DeserializationUtil.deserializeAsList(
                kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList).filteredOn(h -> "DeploymentConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("openshift-with-app-secret");
                assertThat(m.getLabels().get("app.openshift.io/runtime")).isEqualTo("quarkus");
            });

            AbstractObjectAssert<?, ?> specAssert = assertThat(h).extracting("spec");
            specAssert.extracting("template").extracting("spec").isInstanceOfSatisfying(PodSpec.class,
                    podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            List<EnvVar> envVars = container.getEnv();
                            assertThat(envVars).anySatisfy(envVar -> {
                                assertThat(envVar.getName()).isEqualTo("SMALLRYE_CONFIG_LOCATIONS");
                                assertThat(envVar.getValue()).isEqualTo("/mnt/app-secret");
                            });

                            List<VolumeMount> mounts = container.getVolumeMounts();
                            assertThat(mounts).anySatisfy(mount -> {
                                assertThat(mount.getName()).isEqualTo("app-secret");
                                assertThat(mount.getMountPath()).isEqualTo("/mnt/app-secret");
                            });
                        });
                        List<Volume> volumes = podSpec.getVolumes();
                        assertThat(volumes).anySatisfy(volume -> {
                            assertThat(volume.getName()).isEqualTo("app-secret");
                            assertThat(volume.getSecret().getSecretName()).isEqualTo("my-secret");
                        });
                    });
        });
    }
}

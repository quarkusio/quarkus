package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithLegacySidecarAndS2iTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift-sidecar-test")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("openshift-with-legacy-sidecar-and-s2i.properties")
            .setForcedDependencies(Collections.singletonList(
                    new AppArtifact("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");

        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> openshiftList = DeserializationUtil.deserializeAsList(
                kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList).filteredOn(h -> "DeploymentConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("openshift-sidecar-test");
                assertThat(m.getLabels().get("app.openshift.io/runtime")).isEqualTo("quarkus");
            });

            AbstractObjectAssert<?, ?> specAssert = assertThat(h).extracting("spec");
            specAssert.extracting("template").extracting("spec").isInstanceOfSatisfying(PodSpec.class,
                    podSpec -> {
                        assertThat(podSpec.getContainers()).hasSize(2);
                        assertSidecar(podSpec);
                        assertApplicationContainer(podSpec);
                    });
        });
    }

    private void assertApplicationContainer(PodSpec podSpec) {
        assertThat(podSpec.getContainers()).filteredOn(ps -> "openshift-sidecar-test".equals(ps.getName()))
                .singleElement().satisfies(c -> {
                    assertThat(c.getEnv()).extracting("name").contains("JAVA_APP_JAR");
                });
    }

    private void assertSidecar(PodSpec podSpec) {
        assertThat(podSpec.getContainers()).filteredOn(ps -> "sc".equals(ps.getName()))
                .singleElement().satisfies(c -> {
                    assertThat(c.getImage()).isEqualTo("quay.io/sidecar/image:2.1");
                    assertThat(c.getImagePullPolicy()).isEqualTo("IfNotPresent");
                    assertThat(c.getCommand()).containsOnly("ls");
                    assertThat(c.getArgs()).containsOnly("-l");
                    assertThat(c.getWorkingDir()).isEqualTo("/work");
                    assertThat(c.getVolumeMounts()).singleElement().satisfies(volumeMount -> {
                        assertThat(volumeMount.getName()).isEqualTo("app-config");
                        assertThat(volumeMount.getMountPath()).isEqualTo("/deployments/config");
                    });
                    assertThat(c.getPorts()).singleElement().satisfies(p -> {
                        assertThat(p.getContainerPort()).isEqualTo(3000);
                    });
                    assertThat(c.getEnv()).extracting("name").doesNotContain("JAVA_APP_JAR");
                });
    }
}

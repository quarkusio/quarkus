package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

//
// The purpose of this test is to assert that
// When: We run local container builds targeting Openshift (and `DeploymentConfig` is used).
// Then:
//   - No BuildConfg is generated.
//   - A signle docker ImageStreams are generated and that `docker.io` is used as the default registry.
//
public class OpenshiftWithLocalDockerTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("openshift-with-local-docker")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.container-image.builder", "docker")
            .overrideConfigKey("quarkus.container-image.group", "testme")
            .setLogFileName("k8s.log")
            .setForcedDependencies(Arrays.asList(new AppArtifact("io.quarkus", "quarkus-openshift", Version.getVersion()),
                    new AppArtifact("io.quarkus", "quarkus-container-image-docker", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(DeploymentConfig.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("openshift-with-local-docker");
            });

            assertThat(kubernetesList).filteredOn(h -> "ImageStream".equals(h.getKind())).hasSize(1)
                    .anySatisfy(h -> {
                        assertThat(h).isInstanceOfSatisfying(ImageStream.class, imageStream -> {
                            assertThat(imageStream.getSpec().getDockerImageRepository())
                                    .isEqualTo("docker.io/testme/openshift-with-local-docker");
                        });
                    });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {

                    });
                });
            });
        });
    }
}

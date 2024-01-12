package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

//
// The purpose of this test is to assert that
// When: We run local container builds targeting Openshift using `Deployment` (instead of `DeploymentConfig`)
// Then: No BuildConfg and ImageStreams are generated and that `docker.io` is used as the default registry
//
public class OpenshiftWithLocalDockerAndDeploymentResourceTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("openshift-with-local-docker-and-deployment-resource")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.container-image.builder", "docker")
            .overrideConfigKey("quarkus.container-image.group", "testme")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-container-image-docker", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("openshift-with-local-docker-and-deployment-resource");
            });

            assertThat(kubernetesList).filteredOn(h -> "BuildConfig".equals(h.getKind())).hasSize(0);
            assertThat(kubernetesList).filteredOn(h -> "ImageStream".equals(h.getKind())).hasSize(0);

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getImage())
                                    .isEqualTo(
                                            "docker.io/testme/openshift-with-local-docker-and-deployment-resource:0.1-SNAPSHOT");
                        });
                    });
                });
            });
        });
    }
}

package io.quarkus.it.kubernetes;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.LogFile;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class BasicKubernetesTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("basic")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setLogFileName("k8s.log");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @LogFile
    private Path logfile;

    @Test
    public void assertApplicationRuns() {
        assertThat(logfile).isRegularFile().hasFileName("k8s.log");
        TestUtil.assertLogFileContents(logfile, "kubernetes");

        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).hasSize(3);

        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("basic");
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).hasOnlyOneElementSatisfying(container -> {
                            assertThat(container.getImagePullPolicy()).isEqualTo("Always"); // expect the default value
                            assertThat(container.getPorts()).hasOnlyOneElementSatisfying(p -> {
                                assertThat(p.getContainerPort()).isEqualTo(8080);
                            });
                        });
                    });
                });
            });
        });

        assertThat(kubernetesList.get(1)).isInstanceOfSatisfying(Service.class, s -> {
            assertThat(s.getSpec()).satisfies(spec -> {
                assertThat(spec.getPorts()).hasSize(1).hasOnlyOneElementSatisfying(p -> {
                    assertThat(p.getPort()).isEqualTo(8080);
                });
            });
        });

        assertThat(kubernetesList.get(2)).isInstanceOf(ServiceAccount.class);
    }
}

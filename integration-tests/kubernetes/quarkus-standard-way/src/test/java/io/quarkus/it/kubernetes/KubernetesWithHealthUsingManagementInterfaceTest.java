package io.quarkus.it.kubernetes;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.LogFile;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithHealthUsingManagementInterfaceTest {

    private static final String NAME = "kubernetes-with-health-and-management";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setLogFileName("k8s.log")
            .withConfigurationResource(NAME + ".properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-health", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @LogFile
    private Path logfile;

    @Test
    public void assertApplicationRuns() {
        assertThat(logfile).isRegularFile().hasFileName("k8s.log");
        TestUtil.assertLogFileContents(logfile, "kubernetes", "health");

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
        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(NAME);
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getReadinessProbe()).isNotNull().satisfies(p -> {
                                assertThat(p.getInitialDelaySeconds()).isEqualTo(5);
                                assertProbePath(p, "/q/health/ready");

                                assertNotNull(p.getHttpGet());
                                assertEquals("HTTP", p.getHttpGet().getScheme());
                                assertEquals(9000, p.getHttpGet().getPort().getIntVal());
                            });
                            assertThat(container.getLivenessProbe()).isNotNull().satisfies(p -> {
                                assertThat(p.getInitialDelaySeconds()).isEqualTo(20);
                                assertProbePath(p, "/liveness");

                                assertNotNull(p.getHttpGet());
                                assertEquals("HTTP", p.getHttpGet().getScheme());
                                assertEquals(9000, p.getHttpGet().getPort().getIntVal());
                            });
                        });
                    });
                });
            });
        });

        assertThat(kubernetesList.get(1)).isInstanceOfSatisfying(Service.class, s -> {
            assertThat(s.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(NAME);
            });

            assertThat(s.getSpec()).satisfies(spec -> {
                assertThat(spec.getPorts()).hasSize(2);
                assertThat(spec.getPorts()).satisfiesOnlyOnce(port -> assertThat(port.getName()).isEqualTo("http"));
                assertThat(spec.getPorts()).satisfiesOnlyOnce(port -> assertThat(port.getName()).isEqualTo("https"));
            });
        });
    }

    private void assertProbePath(Probe p, String expectedPath) {
        assertThat(p.getHttpGet()).satisfies(h -> {
            assertThat(h.getPath()).isEqualTo(expectedPath);
        });
    }
}

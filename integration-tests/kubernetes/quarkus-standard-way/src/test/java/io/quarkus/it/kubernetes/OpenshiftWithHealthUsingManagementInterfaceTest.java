package io.quarkus.it.kubernetes;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.LogFile;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithHealthUsingManagementInterfaceTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift-health")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setLogFileName("k8s.log")
            .withConfigurationResource("openshift-with-health-and-management.properties")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-smallrye-health", Version.getVersion())));

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
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> openshiftList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList).filteredOn(h -> "Deployment".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("openshift-health");
            });
            assertThat(h).extracting("spec").extracting("template").extracting("spec").isInstanceOfSatisfying(PodSpec.class,
                    podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getReadinessProbe()).isNotNull().satisfies(p -> {
                                assertThat(p.getPeriodSeconds()).isEqualTo(15);
                                assertThat(p.getHttpGet()).satisfies(h1 -> {
                                    assertThat(h1.getPath()).isEqualTo("/q/health/ready");
                                    assertThat(h1.getPort()).isEqualTo(new IntOrString(9000));
                                });
                            });
                            assertThat(container.getLivenessProbe()).isNotNull().satisfies(p -> {
                                assertThat(p.getPeriodSeconds()).isEqualTo(10);
                                assertThat(p.getHttpGet()).isNull();
                                assertThat(p.getExec()).satisfies(e -> {
                                    assertThat(e.getCommand()).containsOnly("kill");
                                });
                            });
                        });
                    });
        });
    }

}

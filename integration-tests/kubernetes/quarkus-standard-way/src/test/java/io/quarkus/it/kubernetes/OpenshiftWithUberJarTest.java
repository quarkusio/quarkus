package io.quarkus.it.kubernetes;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.LogFile;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithUberJarTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift-uberjar").setApplicationVersion("0.1-SNAPSHOT").setRun(true)
            .setLogFileName("k8s.log").withConfigurationResource("openshift-with-uberjar.properties")
            .setForcedDependencies(
                    Collections.singletonList(new AppArtifact("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @LogFile
    private Path logfile;

    @Test
    public void assertApplicationRuns() {
        assertThat(logfile).isRegularFile().hasFileName("k8s.log");
        TestUtil.assertLogFileContents(logfile, "kubernetes", "uberjar");

        given().when().get("/greeting").then().statusCode(200).body(is("hello"));
    }

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir).isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> openshiftList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList).filteredOn(h -> "DeploymentConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("openshift-uberjar");
            });
            assertThat(h).extracting("spec").extracting("template").extracting("spec").isInstanceOfSatisfying(PodSpec.class,
                    podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getEnv())
                                    .filteredOn(new Condition<EnvVar>(e -> e.getName().equals("APP_JAVA_LIB"),
                                            "Environment variable JAVA_APP_LIB"))
                                    .hasSize(0);

                            assertThat(container.getArgs())
                                    .filteredOn(new Condition<String>(e -> e.equals("-cp"), "Classpath arguments"))
                                    .hasSize(0);
                        });
                    });
        });
    }

}

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
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.LogFile;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class WithKubernetesClientTest {

    private static final String APP_NAME = "kubernetes-with-client";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true)
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes-client", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @LogFile
    private Path logfile;

    @Test
    public void assertApplicationRuns() {
        assertThat(logfile).isRegularFile().hasFileName("k8s.log");
        TestUtil.assertLogFileContents(logfile, "kubernetes", "kubernetes-client");

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

        assertThat(kubernetesList).filteredOn(h -> "ServiceAccount".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata().getName()).isEqualTo(APP_NAME);
        });

        assertThat(kubernetesList).filteredOn(h -> "RoleBinding".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata().getName()).isEqualTo(APP_NAME + "-view");
            RoleBinding roleBinding = (RoleBinding) h;
            // verify role ref
            assertThat(roleBinding.getRoleRef().getKind()).isEqualTo("ClusterRole");
            assertThat(roleBinding.getRoleRef().getName()).isEqualTo("view");

            // verify subjects
            assertThat(roleBinding.getSubjects()).isNotEmpty();
            Subject subject = roleBinding.getSubjects().get(0);
            assertThat(subject.getKind()).isEqualTo("ServiceAccount");
            assertThat(subject.getName()).isEqualTo(APP_NAME);
        });
    }

    @Test
    public void assertDependencies() {
        Path mainDepsPath = prodModeTestResults.getBuildDir().resolve("quarkus-app").resolve("lib").resolve("main");
        assertThat(mainDepsPath).isDirectoryContaining(p -> p.getFileName().toString().contains("kubernetes-client"));
    }
}

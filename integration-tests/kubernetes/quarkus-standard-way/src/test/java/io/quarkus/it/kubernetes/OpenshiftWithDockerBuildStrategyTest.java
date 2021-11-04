package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithDockerBuildStrategyTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift-s2i").setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("openshift-with-docker-build-strategy.properties")
            .setForcedDependencies(Collections
                    .singletonList(new AppArtifact("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");

        assertThat(kubernetesDir).isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> openshiftList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        //Assert that the container contains neither command nor arguments
        assertThat(openshiftList).filteredOn(d -> "DeploymentConfig".equals(d.getKind())).singleElement().satisfies(d -> {
            assertThat(d).isInstanceOfSatisfying(DeploymentConfig.class, dc -> {
                assertThat(dc.getSpec().getTemplate().getSpec().getContainers()).singleElement().satisfies(c -> {
                    assertThat(c.getCommand()).isNullOrEmpty();
                    assertThat(c.getArgs()).isNullOrEmpty();
                    //We explicitly remove them when using the `docker build strategy`.
                    assertThat(c.getEnv()).extracting("name").doesNotContain("JAVA_APP_JAR");
                });
            });
        });

        assertThat(openshiftList).filteredOn(h -> "BuildConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("openshift-s2i");
                assertThat(m.getLabels().get("app.openshift.io/runtime")).isEqualTo("quarkus");
            });
            assertThat(h).isInstanceOfSatisfying(BuildConfig.class, bc -> {
                assertThat(bc.getSpec().getSource()).satisfies(s -> {
                    assertThat(s.getDockerfile()).isNotNull();
                });
            });
        });

    }
}

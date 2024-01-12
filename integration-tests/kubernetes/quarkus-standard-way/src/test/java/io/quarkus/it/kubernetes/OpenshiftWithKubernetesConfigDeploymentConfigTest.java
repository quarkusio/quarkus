package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithKubernetesConfigDeploymentConfigTest {

    private static final String NAME = "openshift-with-kubernetes-config";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.openshift.deployment-kind", "deployment-config")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-kubernetes-config", Version.getVersion())));

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
                assertThat(m.getName()).isEqualTo(NAME);
                assertThat(m.getLabels().get("app.openshift.io/runtime")).isEqualTo("quarkus");
            });

            AbstractObjectAssert<?, ?> specAssert = assertThat(h).extracting("spec");
            specAssert.extracting("template").extracting("spec").isInstanceOfSatisfying(PodSpec.class,
                    podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            List<EnvVar> envVars = container.getEnv();
                            assertThat(envVars).anySatisfy(envVar -> {
                                assertThat(envVar.getName()).isEqualTo("JAVA_APP_JAR");
                                assertThat(envVar.getValue()).isEqualTo("/deployments/quarkus-run.jar");
                            });
                        });
                    });
        });
    }
}

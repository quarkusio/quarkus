package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithEnvFromConfigMapWithPrefixTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("env-from-config-map-with-prefix")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("openshift-with-env-from-configmap-with-prefix.properties")
            .overrideConfigKey("quarkus.openshift.deployment-kind", "Deployment")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("openshift.yml"));
        assertThat(kubernetesList).filteredOn(i -> i instanceof Deployment).singleElement()
                .isInstanceOfSatisfying(Deployment.class, d -> {
                    assertThat(d.getMetadata()).satisfies(m -> {
                        assertThat(m.getName()).isEqualTo("env-from-config-map-with-prefix");
                    });

                    assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                        assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                            assertThat(t.getSpec()).satisfies(podSpec -> {
                                assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {

                                    assertThat(container.getEnvFrom()).satisfies(env -> {

                                        assertThat(env).anyMatch(item -> item.getPrefix().equals("QUARKUS") &&
                                                item.getConfigMapRef().getName().equals("my-config-map"));

                                    });
                                });
                            });
                        });
                    });
                });
    }
}

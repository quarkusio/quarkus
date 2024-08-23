
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

public class KubernetesWithStrategyRollingUpdateTest {

    private static final String APP_NAME = "kubernetes-with-strategy-rolling-update";
    private static final String STRATEGY_TYPE = "RollingUpdate";
    private static final String MAX_UNAVAILABLE = "35%";
    private static final String MAX_SURGE = "39%";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.strategy", STRATEGY_TYPE)
            .overrideConfigKey("quarkus.kubernetes.rolling-update.max-unavailable", MAX_UNAVAILABLE)
            .overrideConfigKey("quarkus.kubernetes.rolling-update.max-surge", MAX_SURGE)
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

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
                assertThat(m.getName()).isEqualTo(APP_NAME);
            });

            assertThat(d.getSpec()).satisfies(spec -> {
                assertThat(spec.getStrategy().getType()).isEqualTo(STRATEGY_TYPE);
                assertThat(spec.getStrategy().getRollingUpdate()).isNotNull();
                assertThat(spec.getStrategy().getRollingUpdate().getMaxUnavailable().getStrVal()).isEqualTo(MAX_UNAVAILABLE);
                assertThat(spec.getStrategy().getRollingUpdate().getMaxSurge().getStrVal()).isEqualTo(MAX_SURGE);
            });
        });
    }
}

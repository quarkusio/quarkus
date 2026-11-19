package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class EnvFromPrecedenceTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("envfrom-precedence-test")
            .setApplicationVersion("1.0.0")
            .setRun(false)
            .overrideConfigKey("quarkus.kubernetes.env.configmaps", "z-config,a-config,m-config")
            .overrideConfigKey("quarkus.kubernetes.liveness-probe.enabled", "false")
            .overrideConfigKey("quarkus.kubernetes.readiness-probe.enabled", "false");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testEnvFromOrderPreserved() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        Deployment deployment = kubernetesList.stream()
                .filter(r -> r instanceof Deployment)
                .map(r -> (Deployment) r)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Deployment found"));

        Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        List<EnvFromSource> envFromSources = container.getEnvFrom();

        List<String> configMapNames = envFromSources.stream()
                .filter(e -> e.getConfigMapRef() != null)
                .map(e -> e.getConfigMapRef().getName())
                .toList();

        assertThat(configMapNames).containsExactly("z-config", "a-config", "m-config");
    }
}

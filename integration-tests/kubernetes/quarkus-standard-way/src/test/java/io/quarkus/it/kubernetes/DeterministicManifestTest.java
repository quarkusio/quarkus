package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class DeterministicManifestTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("determinism-test")
            .setApplicationVersion("1.0.0")
            .setRun(false)
            // Ports - intentionally unordered
            .overrideConfigKey("quarkus.kubernetes.ports.zebra-port.container-port", "9999")
            .overrideConfigKey("quarkus.kubernetes.ports.alpha-port.container-port", "9001")
            .overrideConfigKey("quarkus.kubernetes.ports.mike-port.container-port", "9090")
            .overrideConfigKey("quarkus.kubernetes.ports.bravo-port.container-port", "8443")
            // Environment variables - intentionally unordered
            .overrideConfigKey("quarkus.kubernetes.env.vars.ZEBRA_VAR", "z")
            .overrideConfigKey("quarkus.kubernetes.env.vars.ALPHA_VAR", "a")
            .overrideConfigKey("quarkus.kubernetes.env.vars.MIKE_VAR", "m")
            .overrideConfigKey("quarkus.kubernetes.env.vars.BRAVO_VAR", "b")
            // Disable health probes to avoid port conflicts
            .overrideConfigKey("quarkus.kubernetes.liveness-probe.enabled", "false")
            .overrideConfigKey("quarkus.kubernetes.readiness-probe.enabled", "false");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testContainerPortsSorted() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        Deployment deployment = kubernetesList.stream()
                .filter(r -> r instanceof Deployment)
                .map(r -> (Deployment) r)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Deployment found"));

        Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        List<ContainerPort> ports = container.getPorts();

        // Extract port names
        List<String> portNames = ports.stream()
                .map(ContainerPort::getName)
                .filter(name -> name.endsWith("-port"))
                .toList();

        // Verify they are sorted alphabetically
        assertThat(portNames).containsExactly("alpha-port", "bravo-port", "mike-port", "zebra-port");
    }

    @Test
    public void testEnvironmentVariablesSorted() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        Deployment deployment = kubernetesList.stream()
                .filter(r -> r instanceof Deployment)
                .map(r -> (Deployment) r)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Deployment found"));

        Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        List<EnvVar> envVars = container.getEnv();

        // Extract our test env vars
        List<String> testEnvVars = envVars.stream()
                .map(EnvVar::getName)
                .filter(name -> name.endsWith("_VAR"))
                .toList();

        // Verify they are sorted alphabetically
        assertThat(testEnvVars).containsExactly("ALPHA_VAR", "BRAVO_VAR", "MIKE_VAR", "ZEBRA_VAR");

        // KUBERNETES_NAMESPACE is always first, then user vars are sorted
        // Verify expected order
        assertThat(envVars).extracting(EnvVar::getName)
                .containsExactly("KUBERNETES_NAMESPACE", "ALPHA_VAR", "BRAVO_VAR", "MIKE_VAR", "ZEBRA_VAR");
    }
}

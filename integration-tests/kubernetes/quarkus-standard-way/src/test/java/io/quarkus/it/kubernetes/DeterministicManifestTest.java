package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
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
            .overrideConfigKey("quarkus.kubernetes.ports.zebra-port.container-port", "9999")
            .overrideConfigKey("quarkus.kubernetes.ports.alpha-port.container-port", "9001")
            .overrideConfigKey("quarkus.kubernetes.ports.mike-port.container-port", "9090")
            .overrideConfigKey("quarkus.kubernetes.ports.bravo-port.container-port", "8443")
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

        List<String> portNames = ports.stream()
                .map(ContainerPort::getName)
                .filter(name -> name.endsWith("-port"))
                .toList();

        assertThat(portNames).containsExactly("alpha-port", "bravo-port", "mike-port", "zebra-port");

        assertThat(ports).extracting(ContainerPort::getContainerPort)
                .contains(8080, 9001, 8443, 9090, 9999);
    }
}

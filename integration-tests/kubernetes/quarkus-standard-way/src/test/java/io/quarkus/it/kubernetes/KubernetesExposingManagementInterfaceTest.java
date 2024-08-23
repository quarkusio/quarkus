package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesExposingManagementInterfaceTest {

    private static final String NAME = "kubernetes-exposing-management";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.management.enabled", "true")
            .overrideConfigKey("quarkus.kubernetes.ingress.expose", "true")
            .overrideConfigKey("quarkus.kubernetes.ingress.target-port", "management");

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

        Service service = kubernetesList.stream().filter(Service.class::isInstance).map(Service.class::cast).findFirst().get();

        assertThat(service.getMetadata()).satisfies(m -> {
            assertThat(m.getName()).isEqualTo(NAME);
        });

        assertThat(service.getSpec()).satisfies(spec -> {
            assertThat(spec.getPorts()).hasSize(2);
            assertThat(spec.getPorts()).satisfiesOnlyOnce(port -> assertThat(port.getName()).isEqualTo("http"));
            assertThat(spec.getPorts()).satisfiesOnlyOnce(port -> assertThat(port.getName()).isEqualTo("management"));
        });
    }
}

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

public class KubernetesWithOptionalEnvFromTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("optional-env-from")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-optional-env-from.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));
        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            List<Container> containers = d.getSpec().getTemplate().getSpec().getContainers();

            assertThat(containers).filteredOn(c -> "optional-env-from".equals(c.getName())).singleElement()
                    .satisfies(container -> {
                        assertThat(configMapRefOptional(container, "my-configmap")).isNull();
                        assertThat(configMapRefOptional(container, "optional-configmap")).isTrue();
                        assertThat(secretRefOptional(container, "my-secret")).isNull();
                        assertThat(secretRefOptional(container, "optional-secret")).isTrue();
                    });

            assertThat(containers).filteredOn(c -> "sc".equals(c.getName())).singleElement()
                    .satisfies(container -> {
                        assertThat(configMapRefOptional(container, "sidecar-configmap")).isNull();
                        assertThat(secretRefOptional(container, "sidecar-optional-secret")).isTrue();
                    });
        });
    }

    private static Boolean configMapRefOptional(Container container, String name) {
        return container.getEnvFrom().stream()
                .filter(e -> e.getConfigMapRef() != null && name.equals(e.getConfigMapRef().getName()))
                .map(EnvFromSource::getConfigMapRef).findFirst().orElseThrow().getOptional();
    }

    private static Boolean secretRefOptional(Container container, String name) {
        return container.getEnvFrom().stream()
                .filter(e -> e.getSecretRef() != null && name.equals(e.getSecretRef().getName()))
                .map(EnvFromSource::getSecretRef).findFirst().orElseThrow().getOptional();
    }
}

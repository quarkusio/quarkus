
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.dekorate.utils.Labels;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithIdempotentTest {

    private static final String APP_NAME = "kubernetes-with-idempotent";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())));

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = Paths.get("target").resolve(APP_NAME);
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).allSatisfy(resource -> {
            assertThat(resource.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(APP_NAME);
                assertThat(m.getAnnotations().get("app.quarkus.io/quarkus-version")).isNotBlank();
                assertThat(m.getAnnotations().get("app.quarkus.io/commit-id")).isNull();
                assertThat(m.getAnnotations().get("app.quarkus.io/build-timestamp")).isNull();
            });

            if (resource instanceof Deployment) {
                Deployment deployment = (Deployment) resource;
                assertThat(deployment.getSpec().getSelector().getMatchLabels()).doesNotContainKey(Labels.VERSION);
                assertThat(deployment.getSpec().getTemplate().getMetadata().getLabels()).doesNotContainKey(Labels.VERSION);
            }
        });
    }
}

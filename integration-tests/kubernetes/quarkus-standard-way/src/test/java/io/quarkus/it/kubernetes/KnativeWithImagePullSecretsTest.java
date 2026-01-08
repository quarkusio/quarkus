package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeWithImagePullSecretsTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("knative-with-image-pull-secrets")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.deployment-target", "knative")
            .overrideConfigKey("quarkus.knative.image-pull-secrets", "my-secret")
            .overrideConfigKey("quarkus.knative.add-image-pull-secrets-to-service-account", "false")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("knative.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Service".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("knative-with-image-pull-secrets");
                });

                assertThat(s.getSpec()).satisfies(spec -> {
                    assertThat(spec.getTemplate()).satisfies(template -> {
                        assertThat(template.getSpec()).satisfies(revisionSpec -> {
                            assertThat(revisionSpec.getImagePullSecrets()).hasSize(1);
                            assertThat(revisionSpec.getImagePullSecrets().get(0).getName()).isEqualTo("my-secret");
                        });
                    });
                });
            });
        });

        // Ensure no ServiceAccount was generated
        assertThat(kubernetesList).filteredOn(i -> "ServiceAccount".equals(i.getKind())).isEmpty();
    }
}

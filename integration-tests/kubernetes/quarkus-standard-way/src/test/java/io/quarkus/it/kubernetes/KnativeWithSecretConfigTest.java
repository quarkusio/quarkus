
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeWithSecretConfigTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("knative-with-secret-config")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("knative-with-secret-config.properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(Arrays.asList(new AppArtifact("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    new AppArtifact("io.quarkus", "quarkus-kubernetes-config", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("knative.yml"));

        assertThat(kubernetesList).filteredOn(h -> "RoleBinding".equals(h.getKind())).hasSize(2);
        assertThat(kubernetesList).filteredOn(h -> "ServiceAccount".equals(h.getKind())).singleElement().satisfies(s -> {
            assertThat(s.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("knative-with-secret-config");
            });
        });

        assertThat(kubernetesList).filteredOn(h -> "Service".equals(h.getKind())).singleElement().isInstanceOf(Service.class)
                .satisfies(s -> {
                    assertThat(s.getMetadata()).satisfies(m -> {
                        assertThat(m.getName()).isEqualTo("knative-with-secret-config");
                    });
                    assertThat(((Service) s).getSpec()).satisfies(serviceSpec -> {
                        assertThat(serviceSpec.getTemplate()).satisfies(revisionTemplateSpec -> {
                            assertThat(revisionTemplateSpec.getSpec()).satisfies(revisionSpec -> {
                                assertThat(revisionSpec.getServiceAccountName()).isEqualTo("knative-with-secret-config");
                            });
                        });
                    });
                });
    }
}

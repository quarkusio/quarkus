
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.dekorate.servicebinding.model.ServiceBinding;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithServiceBindingTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift-with-service-binding")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("openshift-with-service-binding.properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(
                    Arrays.asList(new AppArtifact("io.quarkus", "quarkus-openshift", Version.getVersion()),
                            new AppArtifact("io.quarkus", "quarkus-kubernetes-service-binding", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(kubernetesList).filteredOn(i -> "DeploymentConfig".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(DeploymentConfig.class, d -> {
                assertThat(d.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("openshift-with-service-binding");
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> "ServiceBinding".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(ServiceBinding.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("openshift-with-service-binding");
                });
                assertThat(s.getSpec()).satisfies(spec -> {
                    assertThat(spec.getApplication()).satisfies(a -> {
                        assertThat(a.getGroup()).isEqualTo("apps.openshift.io");
                        assertThat(a.getVersion()).isEqualTo("v1");
                        assertThat(a.getKind()).isEqualTo("DeploymentConfig");
                    });

                    assertThat(spec.getServices()).hasOnlyOneElementSatisfying(service -> {
                        assertThat(service.getGroup()).isEqualTo("apps");
                        assertThat(service.getVersion()).isEqualTo("v1");
                        assertThat(service.getKind()).isEqualTo("Deployment");
                        assertThat(service.getName()).isEqualTo("my-postgres");
                    });
                });
            });
        });
    }
}


package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
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
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-kubernetes-service-binding", Version.getVersion())));

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

        assertThat(kubernetesList).filteredOn(i -> "ServiceBinding".equals(i.getKind())).singleElement()
                .isInstanceOfSatisfying(GenericKubernetesResource.class, sb -> assertThat(sb)
                        .hasFieldOrPropertyWithValue("metadata.name", "openshift-with-service-binding-my-db")
                        .returns("apps.openshift.io", s -> s.get("spec", "application", "group"))
                        .returns("v1", s -> s.get("spec", "application", "version"))
                        .returns("DeploymentConfig", s -> s.get("spec", "application", "kind"))
                        .extracting(s -> s.get("spec", "services"))
                        .asList()
                        .singleElement().asInstanceOf(InstanceOfAssertFactories.MAP)
                        .containsEntry("group", "apps")
                        .containsEntry("version", "v1")
                        .containsEntry("kind", "Deployment")
                        .containsEntry("name", "my-postgres"));
    }
}

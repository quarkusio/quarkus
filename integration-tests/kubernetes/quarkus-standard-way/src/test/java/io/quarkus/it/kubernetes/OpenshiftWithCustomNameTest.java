
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

//
// The purpose of this test is to assert that
// When: We run an in-cluster  container builds targeting Openshift (and `Deployment` is used).
// Then:
//   - A BuildConfg is generated.
//   - Two ImageStream are generated (one named after the app).
//   - A Deployment resource was created
//   - image-registry.openshift-image-registry.svc:5000 is used as registry (why? so that Deployment can point to the incluster built image).
//
public class OpenshiftWithCustomNameTest {

    private static final String NAME = "openshift-with-custom-name";
    private static final String CUSTOM_NAME = "custom-name";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.openshift.name", "custom-name")
            .overrideConfigKey("quarkus.openshift.deployment-kind", "Deployment")
            .overrideConfigKey("quarkus.container-image.group", "testme")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion())));

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

        assertThat(kubernetesList).filteredOn(h -> "BuildConfig".equals(h.getKind())).hasSize(1);
        assertThat(kubernetesList).filteredOn(h -> "ImageStream".equals(h.getKind())).hasSize(2);
        assertThat(kubernetesList).filteredOn(h -> "ImageStream".equals(h.getKind())
                && h.getMetadata().getName().equals(CUSTOM_NAME)).hasSize(1);

        assertThat(kubernetesList).filteredOn(i -> i instanceof Deployment).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Deployment.class, d -> {
                assertThat(d.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("custom-name");
                });

                assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                    assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                        assertThat(t.getMetadata()).satisfies(metadata -> assertThat(metadata.getLabels()).containsAnyOf(
                                entry("app.kubernetes.io/name", CUSTOM_NAME),
                                entry("app.kubernetes.io/version", "0.1-SNAPSHOT")));
                    });
                });
            });
        });
    }
}


package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithRbacAndNamespaceTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("kubernetes-with-rbac-and-namespace")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("kubernetes-with-rbac-and-namespace.properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(Arrays.asList(new AppArtifact("io.quarkus", "quarkus-kubernetes", Version.getVersion()),
                    new AppArtifact("io.quarkus", "quarkus-kubernetes-client", Version.getVersion())));

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

        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Deployment.class, d -> {
            assertThat(d.getMetadata()).satisfies(m -> {
                assertThat(m.getLabels()).contains(entry("foo", "bar"));
                assertThat(m.getName()).isEqualTo("kubernetes-with-rbac-and-namespace");
            });

            assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {

                    });
                });
            });
        });

        Optional<RoleBinding> roleBinding = kubernetesList.stream().filter(r -> r instanceof RoleBinding)
                .map(r -> (RoleBinding) r).findFirst();
        assertThat(roleBinding).satisfies(o -> {
            assertThat(o.isPresent()).isTrue();
            assertThat(o.get()).satisfies(rb -> {
                Optional<Subject> violating = rb.getSubjects().stream()
                        .filter(s -> s.getNamespace() == null || !s.getNamespace().equals("my-namespace"))
                        .findAny();
                assertThat(violating.isPresent()).isFalse();
            });
        });
    }
}

package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeWithExtendedPropertiesTest {

    private static final String APP_NAME = "knative-with-extended-properties";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.deployment-target", "knative")
            .overrideConfigKey("quarkus.knative.revision-auto-scaling.container-concurrency", "5")
            .overrideConfigKey("quarkus.knative.min-scale", "5")
            .overrideConfigKey("quarkus.knative.max-scale", "10")
            .overrideConfigKey("quarkus.knative.image-pull-policy", "Never")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())));

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
        assertThat(kubernetesList.get(0)).isInstanceOfSatisfying(Service.class, s -> {
            assertThat(s.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(APP_NAME);
            });

            assertThat(s.getSpec()).satisfies(serviceSpec -> {
                assertThat(serviceSpec.getTemplate()).satisfies(template -> {
                    assertThat(template.getMetadata()).satisfies(m -> {
                        assertThat(m.getAnnotations()).contains(entry("autoscaling.knative.dev/min-scale", "5"));
                        assertThat(m.getAnnotations()).contains(entry("autoscaling.knative.dev/max-scale", "10"));
                    });
                    assertThat(template.getSpec()).satisfies(revisionSpec -> {
                        assertThat(revisionSpec.getContainerConcurrency()).isEqualTo(5);

                        assertThat(revisionSpec.getContainers().get(0)).satisfies(c -> {
                            assertThat(c.getImagePullPolicy()).isEqualTo("Never");
                        });
                    });
                });
            });
        });
    }
}

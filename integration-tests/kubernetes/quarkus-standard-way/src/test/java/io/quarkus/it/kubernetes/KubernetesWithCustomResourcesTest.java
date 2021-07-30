package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.BuildContext;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestBuildStep;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithCustomResourcesTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("custom-resources")
            .setApplicationVersion("0.1-SNAPSHOT")
            .addCustomResourceEntry(Path.of("src", "main", "kubernetes", "kubernetes.yml"),
                    "manifests/custom-deployment/kubernetes.yml")
            .addBuildChainCustomizerEntries(
                    new QuarkusProdModeTest.BuildChainCustomizerEntry(CustomProjectRootBuildItemProducerProdMode.class,
                            Collections.singletonList(CustomProjectRootBuildItem.class), Collections.emptyList()));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(2));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).filteredOn(h -> h.getMetadata().getName().equals("kubernetes-with-existing-manifest"))
                .singleElement()
                .isInstanceOfSatisfying(Deployment.class, d -> {
                    assertThat(d.getMetadata()).satisfies(m -> {
                        assertThat(m.getName()).isEqualTo("kubernetes-with-existing-manifest");
                        assertThat(m.getNamespace()).isNull();
                        assertThat(m.getLabels()).containsOnly(entry("app", "quickstart"));
                    });

                    assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                        assertThat(deploymentSpec.getReplicas()).isEqualTo(3);
                        assertThat(deploymentSpec.getSelector()).isNotNull().satisfies(labelSelector -> {
                            assertThat(labelSelector.getMatchLabels()).containsOnly(entry("app", "quickstart"));
                        });

                        assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                            assertThat(t.getSpec()).satisfies(podSpec -> {
                                assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                                    assertThat(container.getName()).isEqualTo("kubernetes-with-existing-manifest");
                                    assertThat(container.getPorts()).singleElement().satisfies(p -> {
                                        assertThat(p.getContainerPort()).isEqualTo(80);
                                    });
                                    assertThat(container.getEnv()).singleElement().satisfies(envVar -> {
                                        assertThat(envVar.getName()).isEqualTo("FOO");
                                        assertThat(envVar.getValue()).isEqualTo("BAR");
                                    });
                                });
                            });
                        });
                    });
                });

        assertThat(kubernetesList).filteredOn(h -> h instanceof Service).singleElement().isInstanceOfSatisfying(Service.class,
                s -> {
                    assertThat(s.getMetadata()).satisfies(m -> {
                        assertThat(m.getNamespace()).isNull();
                    });
                    assertThat(s.getSpec()).satisfies(spec -> {
                        assertThat(spec.getSelector()).containsOnly(entry("app.kubernetes.io/name", "custom-resources"),
                                entry("app.kubernetes.io/version", "0.1-SNAPSHOT"));

                        assertThat(spec.getPorts()).hasSize(1).singleElement().satisfies(p -> {
                            assertThat(p.getPort()).isEqualTo(80);
                            assertThat(p.getTargetPort().getIntVal()).isEqualTo(8080);
                        });
                    });
                });
    }

    public static class CustomProjectRootBuildItemProducerProdMode extends ProdModeTestBuildStep {

        public CustomProjectRootBuildItemProducerProdMode(Map<String, Object> testContext) {
            super(testContext);
        }

        @Override
        public void execute(BuildContext context) {
            context.produce(new CustomProjectRootBuildItem(
                    (Path) getTestContext().get(QuarkusProdModeTest.BUILD_CONTEXT_CUSTOM_SOURCES_PATH_KEY)));
        }
    }
}

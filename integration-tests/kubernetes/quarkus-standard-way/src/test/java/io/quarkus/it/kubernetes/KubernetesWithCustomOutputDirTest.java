package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.BuildContext;
import io.quarkus.kubernetes.spi.CustomKubernetesOutputDirBuildItem;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestBuildStep;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithCustomOutputDirTest {

    private static final String APP_NAME = "kubernetes-with-custom-output-dir";
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .addBuildChainCustomizerEntries(
                    new QuarkusProdModeTest.BuildChainCustomizerEntry(CustomProjectRootBuildItemProducerProdMode.class,
                            List.of(CustomProjectRootBuildItem.class, CustomKubernetesOutputDirBuildItem.class),
                            Collections.emptyList()));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().getParent().resolve("custom-sources")
                .resolve(".kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(2));
        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));
        assertThat(kubernetesList).filteredOn(h -> h.getMetadata().getName().equals(APP_NAME))
                .filteredOn(e -> e instanceof Deployment).singleElement().satisfies(d -> {
                    assertThat(d.getMetadata()).satisfies(m -> {
                        assertThat(m.getName()).isEqualTo(APP_NAME);
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
            context.produce(new CustomKubernetesOutputDirBuildItem(
                    ((Path) getTestContext().get(QuarkusProdModeTest.BUILD_CONTEXT_CUSTOM_SOURCES_PATH_KEY))
                            .resolve(".kubernetes")));
        }
    }
}

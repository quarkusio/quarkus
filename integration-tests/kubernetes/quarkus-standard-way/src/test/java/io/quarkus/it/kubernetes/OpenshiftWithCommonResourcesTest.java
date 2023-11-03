package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.Version;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestBuildStep;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithCommonResourcesTest {

    private static final String APP_NAME = "openshift-with-common-resources";
    private static final String COMMON_CONFIGMAP_NAME = "common-configmap";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .addCustomResourceEntry(Path.of("src", "main", "kubernetes", "common.yml"),
                    "manifests/" + APP_NAME + "/common.yml")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion())))
            .addBuildChainCustomizerEntries(
                    new QuarkusProdModeTest.BuildChainCustomizerEntry(CustomProjectRootBuildItemProducerProdMode.class,
                            Collections.singletonList(CustomProjectRootBuildItem.class), Collections.emptyList()));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("kubernetes.yml"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(4));
        assertThat(DeserializationUtil.deserializeAsList(kubernetesDir.resolve("openshift.yml")))
                .filteredOn(h -> h instanceof ConfigMap)
                .singleElement()
                .isInstanceOfSatisfying(ConfigMap.class, c -> c.getMetadata().getName().equals(COMMON_CONFIGMAP_NAME));
        assertThat(DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml")))
                .filteredOn(h -> h instanceof ConfigMap)
                .singleElement()
                .isInstanceOfSatisfying(ConfigMap.class, c -> c.getMetadata().getName().equals(COMMON_CONFIGMAP_NAME));
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

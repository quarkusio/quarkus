package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.Route;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.Version;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestBuildStep;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithCustomRouteResourceTest {

    private static final String APP_NAME = "openshift-with-custom-route-resource";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties")
            .addCustomResourceEntry(Path.of("src", "main", "kubernetes", "openshift.yml"),
                    "manifests/" + APP_NAME + "/openshift.yml")
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
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(2));
        List<HasMetadata> openshiftList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("openshift.yml"));
        assertThat(openshiftList).filteredOn(i -> "Route".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Route.class, r -> {
                assertThat(r.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo(APP_NAME);
                    assertThat(m.getLabels()).contains(entry("foo", "bar"));
                    assertThat(m.getAnnotations()).contains(entry("bar", "baz"));
                    assertThat(m.getAnnotations()).contains(entry("kubernetes.io/tls-acme", "true"));
                    assertThat(m.getLabels()).contains(entry("test-label", "test-value"));
                });
                assertThat(r.getSpec().getPort().getTargetPort().getStrVal()).isEqualTo("http");
                assertThat(r.getSpec().getTo().getKind()).isEqualTo("Service");
                assertThat(r.getSpec().getTo().getName()).isEqualTo(APP_NAME);
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

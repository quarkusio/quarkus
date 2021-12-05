package io.quarkus.it.kubernetes.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class BasicOpenshiftTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(DummyProcessor.class))
            .setApplicationName("basic-openshift")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("basic-openshift.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @SuppressWarnings("unchecked")
    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(2));
        List<HasMetadata> openshiftList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList).filteredOn(h -> "DeploymentConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("basic-openshift");
                assertThat(m.getLabels().get("app.openshift.io/runtime")).isEqualTo("quarkus");
                assertThat(m.getNamespace()).isNull();
            });
            AbstractObjectAssert<?, ?> specAssert = assertThat(h).extracting("spec");
            specAssert.extracting("replicas").isEqualTo(1);
            specAssert.extracting("triggers").isInstanceOfSatisfying(Collection.class, c -> {
                assertThat(c).isEmpty();
            });
            specAssert.extracting("selector").isInstanceOfSatisfying(Map.class, selectorsMap -> {
                assertThat(selectorsMap).containsOnly(entry("app.kubernetes.io/name", "basic-openshift"),
                        entry("app.kubernetes.io/version", "0.1-SNAPSHOT"));
            });
            specAssert.extracting("template").isInstanceOfSatisfying(PodTemplateSpec.class, templateMap -> {
                assertThat(templateMap.getSpec()).satisfies(podSpec -> {
                    assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                        assertThat(container.getPorts()).isNullOrEmpty();
                    });
                });
            });
        });
    }
}

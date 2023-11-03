package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftV3Test {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift-v3")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("openshift-v3.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

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
                assertThat(m.getName()).isEqualTo("openshift-v3");
                assertThat(m.getLabels().get("app.openshift.io/runtime")).isEqualTo("quarkus");
                assertThat(m.getLabels().get("app.kubernetes.io/name")).isEqualTo("openshift-v3");
                assertThat(m.getLabels().get("app")).isEqualTo("openshift-v3");
                assertThat(m.getNamespace()).isNull();
            });
            AbstractObjectAssert<?, ?> specAssert = assertThat(h).extracting("spec");
            specAssert.extracting("selector").isInstanceOfSatisfying(Map.class, selectorsMap -> {
                assertThat(selectorsMap).containsOnly(entry("app.kubernetes.io/name", "openshift-v3"),
                        entry("app.kubernetes.io/version", "0.1-SNAPSHOT"));
            });
        });

        assertThat(openshiftList).filteredOn(h -> "Service".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getNamespace()).isNull();
                    assertThat(m.getLabels().get("app.kubernetes.io/name")).isEqualTo("openshift-v3");
                    assertThat(m.getLabels().get("app")).isEqualTo("openshift-v3");
                });

                assertThat(s.getSpec()).satisfies(spec -> {
                    assertThat(spec.getSelector()).containsOnly(entry("app.kubernetes.io/name", "openshift-v3"),
                            entry("app.kubernetes.io/version", "0.1-SNAPSHOT"));
                });
            });
        });
    }
}

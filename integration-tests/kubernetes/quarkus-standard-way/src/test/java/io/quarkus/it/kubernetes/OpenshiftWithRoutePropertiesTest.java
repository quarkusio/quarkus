package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithRoutePropertiesTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("openshift-with-route.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> openshiftList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList).filteredOn(h -> "Service".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getMetadata()).satisfies(m -> {
                    assertThat(m.getNamespace()).isEqualTo("applications");
                });

                assertThat(s.getSpec()).satisfies(spec -> {
                    assertThat(spec.getSelector()).contains(entry("app.kubernetes.io/name", "test-it"));
                    assertThat(spec.getPorts()).hasSize(1).singleElement().satisfies(p -> {
                        assertThat(p.getPort()).isEqualTo(80);
                        assertThat(p.getTargetPort().getIntVal()).isEqualTo(9090);
                    });
                });
            });
        });

        assertThat(openshiftList).filteredOn(i -> "Route".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Route.class, r -> {
                //Check that labels and annotations are also applied to Routes (#10260)
                assertThat(r.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("test-it");
                    assertThat(m.getLabels()).contains(entry("foo", "bar"));
                    assertThat(m.getAnnotations()).contains(entry("bar", "baz"));
                    assertThat(m.getAnnotations()).contains(entry("kubernetes.io/tls-acme", "true"));
                    assertThat(m.getNamespace()).isEqualTo("applications");
                });
                assertThat(r.getSpec().getPort().getTargetPort().getIntVal()).isEqualTo(9090);
                assertThat(r.getSpec().getHost()).isEqualTo("foo.bar.io");
            });
        });
    }
}

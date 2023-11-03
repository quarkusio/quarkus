
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithIngressTargetPortTest {

    private static final String APP_NAME = "kubernetes-with-ingress-target-port";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource(APP_NAME + ".properties")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())));

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
                assertThat(m.getName()).isEqualTo(APP_NAME);
            });

            assertThat(d.getSpec().getTemplate().getSpec().getContainers()).satisfiesOnlyOnce(c -> {
                assertThat(c.getPorts()).hasSize(2);
                assertThat(c.getPorts()).anySatisfy(port -> {
                    assertThat(port.getName()).isEqualTo("http");
                    assertThat(port.getContainerPort()).isEqualTo(8080);
                });
                assertThat(c.getPorts()).anySatisfy(port -> {
                    assertThat(port.getName()).isEqualTo("https");
                    assertThat(port.getContainerPort()).isEqualTo(7777);
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> "Service".equals(i.getKind())).singleElement().satisfies(item -> {
            assertThat(item).isInstanceOfSatisfying(Service.class, service -> {
                assertThat(service.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo(APP_NAME);
                });

                assertThat(service.getSpec().getPorts()).hasSize(2);
                assertThat(service.getSpec().getPorts()).anySatisfy(port -> {
                    assertThat(port.getName()).isEqualTo("http");
                    assertThat(port.getTargetPort().getIntVal()).isEqualTo(8080);
                });
                assertThat(service.getSpec().getPorts()).anySatisfy(port -> {
                    assertThat(port.getName()).isEqualTo("https");
                    assertThat(port.getTargetPort().getIntVal()).isEqualTo(7777);
                });
            });
        });

        assertThat(kubernetesList).filteredOn(i -> "Ingress".equals(i.getKind())).singleElement().satisfies(item -> {
            assertThat(item).isInstanceOfSatisfying(Ingress.class, ingress -> {
                assertThat(ingress.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo(APP_NAME);
                });

                assertThat(ingress.getSpec().getRules()).allSatisfy(rule -> {
                    assertThat(rule.getHttp().getPaths()).allSatisfy(path -> {
                        assertThat(path.getBackend().getService().getPort().getName()).isEqualTo("https");
                    });
                });
            });
        });
    }
}

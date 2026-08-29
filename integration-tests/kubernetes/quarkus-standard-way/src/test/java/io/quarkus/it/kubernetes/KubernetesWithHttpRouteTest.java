package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithHttpRouteTest {

    private static final String APP_NAME = "kubernetes-with-http-route";

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

        assertThat(kubernetesList).filteredOn(i -> "HTTPRoute".equals(i.getKind())).singleElement().satisfies(item -> {
            assertThat(item).isInstanceOfSatisfying(HTTPRoute.class, route -> {
                assertThat(route.getMetadata().getName()).isEqualTo(APP_NAME);
                assertThat(route.getMetadata().getAnnotations())
                        .containsEntry("example.com/gate", "enabled");
                assertThat(route.getSpec().getParentRefs()).singleElement().satisfies(ref -> {
                    assertThat(ref.getName()).isEqualTo("shared-gateway");
                    assertThat(ref.getNamespace()).isEqualTo("infra");
                    assertThat(ref.getKind()).isEqualTo("Gateway");
                });
                assertThat(route.getSpec().getHostnames()).containsExactly("prod.svc.url");
                assertThat(route.getSpec().getRules()).isNotEmpty();
                assertThat(route.getSpec().getRules().get(0).getBackendRefs())
                        .singleElement()
                        .satisfies(backend -> {
                            assertThat(backend.getName()).isEqualTo(APP_NAME);
                            assertThat(backend.getKind()).isEqualTo("Service");
                            assertThat(backend.getPort()).isEqualTo(8080);
                        });
                assertThat(route.getSpec().getRules().get(0).getMatches())
                        .singleElement()
                        .satisfies(match -> {
                            assertThat(match.getPath().getType()).isEqualTo("PathPrefix");
                            assertThat(match.getPath().getValue()).isEqualTo("/");
                        });
            });
        });
    }
}

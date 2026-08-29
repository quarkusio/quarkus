package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.Gateway;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithGatewayTest {

    private static final String APP_NAME = "kubernetes-with-gateway";

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
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Gateway".equals(i.getKind())).singleElement()
                .isInstanceOfSatisfying(Gateway.class, gateway -> {
                    assertThat(gateway.getMetadata().getName()).isEqualTo(APP_NAME);
                    assertThat(gateway.getSpec().getGatewayClassName()).isEqualTo("istio");
                    assertThat(gateway.getSpec().getListeners()).singleElement().satisfies(listener -> {
                        assertThat(listener.getName()).isEqualTo("http");
                        assertThat(listener.getProtocol()).isEqualTo("HTTP");
                        assertThat(listener.getPort()).isEqualTo(80);
                        assertThat(listener.getHostname()).isEqualTo("prod.svc.url");
                    });
                });

        assertThat(kubernetesList).filteredOn(i -> "HTTPRoute".equals(i.getKind())).singleElement()
                .isInstanceOfSatisfying(HTTPRoute.class, route -> {
                    assertThat(route.getSpec().getParentRefs()).singleElement().satisfies(ref -> {
                        assertThat(ref.getName()).isEqualTo(APP_NAME);
                        assertThat(ref.getKind()).isEqualTo("Gateway");
                    });
                    assertThat(route.getSpec().getHostnames()).containsExactly("prod.svc.url");
                });
    }
}

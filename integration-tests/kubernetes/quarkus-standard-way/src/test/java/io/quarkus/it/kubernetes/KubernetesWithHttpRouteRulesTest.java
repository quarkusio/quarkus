package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteRule;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithHttpRouteRulesTest {

    private static final String APP_NAME = "kubernetes-with-http-route-rules";

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
        List<HasMetadata> list = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(list).filteredOn(i -> "HTTPRoute".equals(i.getKind())).singleElement()
                .isInstanceOfSatisfying(HTTPRoute.class, route -> {
                    assertThat(route.getSpec().getHostnames()).containsExactly("prod.svc.url");
                    assertThat(route.getSpec().getRules()).hasSize(3);
                    assertThat(route.getSpec().getRules()).anyMatch(this::defaultRule);
                    assertThat(route.getSpec().getRules()).anyMatch(this::exactPathRule);
                    assertThat(route.getSpec().getRules()).anyMatch(this::customBackendRule);
                });
    }

    private boolean defaultRule(HTTPRouteRule rule) {
        return rule.getMatches().stream().anyMatch(m -> "/prod".equals(m.getPath().getValue())
                && "PathPrefix".equals(m.getPath().getType()))
                && rule.getBackendRefs().stream().anyMatch(b -> APP_NAME.equals(b.getName()));
    }

    private boolean exactPathRule(HTTPRouteRule rule) {
        return rule.getMatches().stream().anyMatch(m -> "/dev".equals(m.getPath().getValue())
                && "Exact".equals(m.getPath().getType()))
                && rule.getBackendRefs().stream().anyMatch(b -> APP_NAME.equals(b.getName()));
    }

    private boolean customBackendRule(HTTPRouteRule rule) {
        return rule.getMatches().stream().anyMatch(m -> "/ea".equals(m.getPath().getValue()))
                && rule.getBackendRefs().stream().anyMatch(b -> "updated-service".equals(b.getName())
                        && Integer.valueOf(8080).equals(b.getPort()));
    }
}

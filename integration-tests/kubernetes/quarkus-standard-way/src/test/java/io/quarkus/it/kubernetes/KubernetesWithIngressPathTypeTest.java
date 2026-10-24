
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithIngressPathTypeTest {

    private static final String APP_NAME = "kubernetes-with-ingress-path-type";
    private static final String INGRESS_HOST = "prod.svc.url";
    private static final String INGRESS_PATH = "/something(/|$)(.*)";

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
        List<HasMetadata> list = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));
        assertThat(list).filteredOn(Ingress.class::isInstance).singleElement().isInstanceOfSatisfying(Ingress.class, i -> {
            assertThat(i.getSpec().getRules()).singleElement().satisfies(rule -> {
                assertThat(rule.getHost()).isEqualTo(INGRESS_HOST);
                assertThat(rule.getHttp().getPaths()).singleElement().satisfies(p -> {
                    assertThat(p.getPath()).isEqualTo(INGRESS_PATH);
                    assertThat(p.getPathType()).isEqualTo("ImplementationSpecific");
                    assertThat(p.getBackend().getService().getName()).isEqualTo(APP_NAME);
                    assertThat(p.getBackend().getService().getPort().getName()).isEqualTo("http");
                });
            });
        });
    }
}

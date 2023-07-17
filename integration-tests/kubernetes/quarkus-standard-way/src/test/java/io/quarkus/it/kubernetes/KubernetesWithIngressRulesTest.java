
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KubernetesWithIngressRulesTest {

    private static final String APP_NAME = "kubernetes-with-ingress-rules";
    private static final String PROD_INGRESS_HOST = "prod.svc.url";
    private static final String DEV_INGRESS_HOST = "dev.svc.url";
    private static final String ALT_INGRESS_HOST = "alt.svc.url";
    private static final String PREFIX = "Prefix";
    private static final String HTTP = "http";

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
        List<HasMetadata> list = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));
        Ingress i = findFirst(list, Ingress.class).orElseThrow(() -> new IllegalStateException());
        assertNotNull(i);
        assertEquals(3, i.getSpec().getRules().size(), "There are more rules than expected");
        assertTrue(i.getSpec().getRules().stream().anyMatch(this::generatedRuleWasUpdated));
        assertTrue(i.getSpec().getRules().stream().anyMatch(this::newRuleWasAdded));
        assertTrue(i.getSpec().getRules().stream().anyMatch(this::newRuleWasAddedWithCustomService));
    }

    private boolean newRuleWasAddedWithCustomService(IngressRule rule) {
        return ALT_INGRESS_HOST.equals(rule.getHost())
                && rule.getHttp().getPaths().size() == 1
                && rule.getHttp().getPaths().stream().anyMatch(p -> p.getPath().equals("/ea")
                        && p.getPathType().equals(PREFIX)
                        && p.getBackend().getService().getName().equals("updated-service")
                        && p.getBackend().getService().getPort().getName().equals("tcp"));
    }

    private boolean newRuleWasAdded(IngressRule rule) {
        return DEV_INGRESS_HOST.equals(rule.getHost())
                && rule.getHttp().getPaths().size() == 1
                && rule.getHttp().getPaths().stream().anyMatch(p -> p.getPath().equals("/dev")
                        && p.getPathType().equals("ImplementationSpecific")
                        && p.getBackend().getService().getName().equals(APP_NAME)
                        && p.getBackend().getService().getPort().getName().equals(HTTP));
    }

    private boolean generatedRuleWasUpdated(IngressRule rule) {
        return PROD_INGRESS_HOST.equals(rule.getHost())
                && rule.getHttp().getPaths().size() == 1
                && rule.getHttp().getPaths().stream().anyMatch(p -> p.getPath().equals("/prod")
                        && p.getPathType().equals(PREFIX)
                        && p.getBackend().getService().getName().equals(APP_NAME)
                        && p.getBackend().getService().getPort().getName().equals(HTTP));
    }

    <T extends HasMetadata> Optional<T> findFirst(List<HasMetadata> list, Class<T> t) {
        return (Optional<T>) list.stream()
                .filter(i -> t.isInstance(i))
                .findFirst();
    }
}

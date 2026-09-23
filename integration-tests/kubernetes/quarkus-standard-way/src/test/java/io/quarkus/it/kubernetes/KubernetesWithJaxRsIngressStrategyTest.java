package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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

/**
 * Verifies that {@code quarkus.kubernetes.ingress.expose-jaxrs-paths=true} generates per-endpoint Ingress rules:
 * static paths use {@code Exact} and paths with template parameters use {@code Prefix}.
 */
public class KubernetesWithJaxRsIngressStrategyTest {

    private static final String APP_NAME = "kubernetes-with-jaxrs-ingress-strategy";
    private static final String HOST = "my-app.example.com";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class, UserResource.class))
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

        List<HasMetadata> list = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("kubernetes.yml"));

        assertThat(list).filteredOn(i -> "Ingress".equals(i.getKind())).singleElement()
                .isInstanceOfSatisfying(Ingress.class, ingress -> {
                    assertThat(ingress.getMetadata().getName()).isEqualTo(APP_NAME);

                    List<IngressRule> rules = ingress.getSpec().getRules();
                    assertThat(rules).hasSize(1);

                    IngressRule rule = rules.get(0);
                    assertThat(rule.getHost()).isEqualTo(HOST);
                    assertThat(rule.getHttp()).isNotNull();
                    assertThat(rule.getHttp().getPaths()).hasSize(2);

                    assertThat(rule.getHttp().getPaths()).anySatisfy(path -> {
                        assertThat(path.getPath()).isEqualTo("/greeting");
                        assertThat(path.getPathType()).isEqualTo("Exact");
                        assertThat(path.getBackend().getService().getName()).isEqualTo(APP_NAME);
                    });

                    assertThat(rule.getHttp().getPaths()).anySatisfy(path -> {
                        assertThat(path.getPath()).isEqualTo("/users");
                        assertThat(path.getPathType()).isEqualTo("Prefix");
                        assertThat(path.getBackend().getService().getName()).isEqualTo(APP_NAME);
                    });
                });
    }
}

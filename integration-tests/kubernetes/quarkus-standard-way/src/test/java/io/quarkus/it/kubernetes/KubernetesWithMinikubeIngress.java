package io.quarkus.it.kubernetes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.assertj.core.api.SoftAssertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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

public class KubernetesWithMinikubeIngress {

    private static final String LOCALHOST = "localhost";
    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("kind-ingress")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setLogFileName("k8s.log")
            //           Configuration provided by issue: https://github.com/quarkusio/quarkus/issues/42294
            .overrideConfigKey("quarkus.kubernetes.ingress.expose", "true")
            .overrideConfigKey("quarkus.kubernetes.ingress.annotations.\"nginx.ingress.kubernetes.io/rewrite-target\"", "/$2")
            .overrideConfigKey("quarkus.kubernetes.ingress.rules.1.host", LOCALHOST)
            .overrideConfigKey("quarkus.kubernetes.ingress.rules.1.path", "/game(/|$)(.*)")
            .overrideConfigKey("quarkus.kubernetes.ingress.rules.1.path-type", "ImplementationSpecific")
            .overrideConfigKey("quarkus.kubernetes.ingress.ingress-class-name", "Nginx")
            .setForcedDependencies(Arrays.asList(
                    Dependency.of("io.quarkus", "quarkus-minikube", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-kubernetes", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    void shouldCreateKindResourcesWithIngressAnnotationsCorrectly() throws IOException {
        final Path kubernetesFile = prodModeTestResults.getBuildDir().resolve("kubernetes").resolve("minikube.yml");
        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesFile);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(kubernetesList).filteredOn(k -> k.getKind().equals("Ingress"))
                    .singleElement().isInstanceOfSatisfying(Ingress.class, ingress -> {

                        softly.assertThat(ingress.getMetadata().getAnnotations())
                                .anySatisfy((key, value) -> {
                                    assertThat(key).isEqualTo("nginx.ingress.kubernetes.io/rewrite-target");
                                    assertThat(value).isEqualTo("/$2");
                                });

                        softly.assertThat(ingress.getSpec()).satisfies(spec -> {

                            softly.assertThat(spec.getIngressClassName()).isEqualTo("Nginx");

                            softly.assertThat(spec.getRules()).hasSize(2);

                            softly.assertThat(spec.getRules()).filteredOn(byLocalhost())
                                    .singleElement()
                                    .satisfies(rule -> {

                                        softly.assertThat(rule.getHttp().getPaths()).hasSize(1);
                                        softly.assertThat(rule.getHttp().getPaths().get(0)).satisfies(path -> {

                                            softly.assertThat(path.getPathType()).isEqualTo("ImplementationSpecific");
                                            softly.assertThat(path.getPath()).isEqualTo("/game(/|$)(.*)");
                                            softly.assertThat(path.getBackend().getService().getPort().getName())
                                                    .isEqualTo("http");
                                            softly.assertThat(path.getBackend().getService().getName())
                                                    .isEqualTo("kind-ingress");

                                        });
                                    });
                        });
                    });
        });
    }

    private static Predicate<IngressRule> byLocalhost() {
        return rule -> rule.getHost() != null && rule.getHost().equals(LOCALHOST);
    }
}

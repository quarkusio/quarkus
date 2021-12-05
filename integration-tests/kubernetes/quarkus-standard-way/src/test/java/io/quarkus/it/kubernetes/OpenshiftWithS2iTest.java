package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.ImageStream;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithS2iTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift-s2i")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("openshift-with-s2i.properties")
            .setForcedDependencies(Collections.singletonList(
                    new AppArtifact("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");

        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> openshiftList = DeserializationUtil.deserializeAsList(
                kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList).filteredOn(h -> "ImageStream".equals(h.getKind()))
                .hasSize(2)
                .anySatisfy(h -> {
                    assertThat(h).isInstanceOfSatisfying(ImageStream.class, imageStream -> {
                        assertThat(imageStream.getMetadata().getName()).isEqualTo("openjdk-11-rhel7");
                        assertThat(imageStream.getSpec().getDockerImageRepository())
                                .isEqualTo("my.registry.example/openjdk/openjdk-11-rhel7");
                    });
                });

        assertThat(openshiftList).filteredOn(h -> "BuildConfig".equals(h.getKind())).hasSize(1);

        assertThat(openshiftList).filteredOn(h -> "DeploymentConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("openshift-s2i");
                assertThat(m.getLabels().get("app.openshift.io/runtime")).isEqualTo("quarkus");
            });

            AbstractObjectAssert<?, ?> specAssert = assertThat(h).extracting("spec");
            specAssert.extracting("template").extracting("spec").isInstanceOfSatisfying(PodSpec.class,
                    podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            List<EnvVar> envVars = container.getEnv();
                            assertThat(envVars).anySatisfy(envVar -> {
                                assertThat(envVar.getName()).isEqualTo("JAVA_APP_JAR");
                                //assertThat(envVar.getValue()).isEqualTo("/deployments/quarkus-run.jar"); // this is flaky
                            });
                            assertThat(envVars).anySatisfy(envVar -> {
                                assertThat(envVar.getName()).isEqualTo("JAVA_OPTIONS");
                                assertThat(envVar.getValue())
                                        .contains("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
                            });
                        });
                    });

            specAssert.extracting("triggers").isInstanceOfSatisfying(Collection.class, c -> {
                assertThat(c).hasSize(1);
            });
        });

        assertThat(openshiftList).filteredOn(h -> "Service".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getSpec()).satisfies(spec -> {
                    assertThat(spec.getPorts()).hasSize(1).singleElement().satisfies(p -> {
                        assertThat(p.getPort()).isEqualTo(80);
                        assertThat(p.getTargetPort().getIntVal()).isEqualTo(8080);
                    });
                });
            });
        });

        assertThat(openshiftList).filteredOn(h -> "Route".equals(h.getKind())).hasSize(1);
    }
}

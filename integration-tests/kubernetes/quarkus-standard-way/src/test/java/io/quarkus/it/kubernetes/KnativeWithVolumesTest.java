package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeWithVolumesTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("knative-with-volumes-properties")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("knative-with-volumes.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(2));

        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("knative.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Service".equals(i.getKind())).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Service.class, s -> {
                assertThat(s.getSpec()).satisfies(spec -> {

                    assertThat(spec.getTemplate()).satisfies(template -> {
                        assertThat(template.getSpec()).satisfies(revisionSpec -> {
                            assertThat(revisionSpec.getVolumes()).haveAtLeastOne(new Condition<Volume>(
                                    v -> v.getName().equals("client-crts")
                                            && v.getSecret().getSecretName().equals("clientcerts"),
                                    "Has secret volume named client-crts referencing secret clientcerts"));
                            assertThat(revisionSpec.getVolumes()).haveAtLeastOne(new Condition<Volume>(
                                    v -> v.getName().equals("client-cfg") && v.getConfigMap().getName().equals("clientconfig"),
                                    "Has config-map named client-cfg referencing configmap clientconfig"));

                            assertThat(revisionSpec.getContainers()).hasSize(1).singleElement().satisfies(c -> {

                                assertThat(c.getVolumeMounts()).haveAtLeastOne(new Condition<VolumeMount>(
                                        m -> m.getName().equals("client-crts"), "Has client-crts mount"));
                                assertThat(c.getVolumeMounts()).haveAtLeastOne(new Condition<VolumeMount>(
                                        m -> m.getName().equals("client-cfg"), "Has client-cfg mount"));

                                assertThat(c.getPorts()).hasSize(1).singleElement().satisfies(p -> {
                                    assertThat(p.getName()).isEqualTo("http1");
                                });
                            });
                        });
                    });
                });
            });
        });
    }
}

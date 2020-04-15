
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeContainerImageTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("knative-with-container-image").setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("knative-with-container-image.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.yml"));

        List<HasMetadata> kubernetesList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("knative.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Service".equals(i.getKind())).hasOnlyOneElementSatisfying(i -> {
            assertThat(i).isInstanceOfSatisfying(Service.class, d -> {

                assertThat(d.getSpec()).satisfies(serviceSpec -> {
                    assertThat(serviceSpec.getTemplate()).satisfies(revisionTemplate -> {
                        assertThat(revisionTemplate.getSpec()).satisfies(spec -> {
                            assertThat(spec.getContainers()).satisfies(containers -> {
                                assertThat(containers.get(0)).satisfies(c -> assertThat(c.getImage())
                                        .isEqualTo("quay.io/grp/knative-with-container-image:0.1-SNAPSHOT"));
                            });
                        });
                    });
                });
            });
        });
    }
}

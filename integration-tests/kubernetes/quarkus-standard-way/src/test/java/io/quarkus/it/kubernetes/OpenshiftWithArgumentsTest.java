package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithArgumentsTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .setApplicationName("openshift-with-arguments")
            .setApplicationVersion("0.1-SNAPSHOT")
            .withConfigurationResource("openshift-with-arguments.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> openshiftList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList.get(0)).isInstanceOfSatisfying(DeploymentConfig.class, dc -> {
            assertThat(dc.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo("openshift-with-arguments");
            });
            assertThat(dc.getSpec()).satisfies(deploymentSpec -> {
                assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                    assertThat(t.getSpec()).satisfies(podSpec -> {
                        assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                            assertThat(container.getName()).isEqualTo("openshift-with-arguments");
                            assertThat(container.getArgs()).containsExactly("A", "B");
                        });
                    });
                });
            });
        });
    }
}

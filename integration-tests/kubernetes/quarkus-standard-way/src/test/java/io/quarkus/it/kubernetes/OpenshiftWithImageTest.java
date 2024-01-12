package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentTriggerImageChangeParams;
import io.fabric8.openshift.api.model.ImageStream;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithImageTest {

    private static final String APP_NAME = "openshift-with-image";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.container-image.image", "user/app:2.0")
            .overrideConfigKey("quarkus.openshift.deployment-kind", "deployment-config")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");

        assertThat(kubernetesDir).isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> openshiftList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(openshiftList).filteredOn(h -> "DeploymentConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(APP_NAME);
            });
            assertThat(h).isInstanceOfSatisfying(DeploymentConfig.class, d -> {
                Container container = d.getSpec().getTemplate().getSpec().getContainers().get(0);
                assertThat(container.getImage()).endsWith("user/app:2.0");

                DeploymentTriggerImageChangeParams imageTriggerParams = d.getSpec().getTriggers().get(0).getImageChangeParams();
                assertThat(imageTriggerParams.getFrom().getKind()).isEqualTo("ImageStreamTag");
                assertThat(imageTriggerParams.getFrom().getName()).isEqualTo(APP_NAME + ":2.0");
            });
        });

        assertThat(openshiftList)
                .filteredOn(h -> "ImageStream".equals(h.getKind()) && h.getMetadata().getName().equals(APP_NAME))
                .singleElement().satisfies(h -> {
                    assertThat(h).isInstanceOfSatisfying(ImageStream.class, i -> {
                        assertThat(i.getSpec().getDockerImageRepository()).isNull();
                    });
                });

    }
}

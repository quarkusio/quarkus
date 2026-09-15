package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.ImageStream;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * With {@code quarkus.container-image.insecure=true}, the generated image streams (the builder image and the
 * application image, both imported through {@code dockerImageRepository}) must be annotated as insecure repositories
 * so that OpenShift imports them from insecure registries.
 */
public class OpenshiftWithInsecureRegistryTest {

    private static final String APP_NAME = "openshift-with-insecure-registry";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.container-image.insecure", "true")
            .overrideConfigKey("quarkus.container-image.registry", "registry.internal:5000")
            .overrideConfigKey("quarkus.container-image.username", "user")
            .overrideConfigKey("quarkus.container-image.password", "pass")
            .overrideConfigKey("quarkus.openshift.base-jvm-image", "registry.internal:5000/base/openjdk:21")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        List<HasMetadata> openshiftList = DeserializationUtil.deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        List<HasMetadata> imageStreams = openshiftList.stream().filter(h -> "ImageStream".equals(h.getKind())).toList();
        assertThat(imageStreams).hasSize(2).allSatisfy(h -> {
            assertThat(h).isInstanceOfSatisfying(ImageStream.class, imageStream -> {
                assertThat(imageStream.getSpec().getDockerImageRepository()).isNotBlank();
                assertThat(imageStream.getMetadata().getAnnotations())
                        .containsEntry("openshift.io/image.insecureRepository", "true");
            });
        });
    }
}

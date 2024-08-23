package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.api.model.BuildConfig;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class OpenshiftWithInternalRegistryTest extends BaseOpenshiftWithRemoteRegistry {

    private static final String APP_NAME = "openshift-with-internal-registry";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(APP_NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.container-image.group", "project")
            .overrideConfigKey("quarkus.container-image.registry", "quay.io")
            .overrideConfigKey("quarkus.openshift.generate-image-pull-secret", "true")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path buildDir = prodModeTestResults.getBuildDir();
        List<HasMetadata> resourceList = getResources("openshift", buildDir);
        assertThat(resourceList).filteredOn(h -> "BuildConfig".equals(h.getKind())).singleElement().satisfies(h -> {
            assertThat(h.getMetadata()).satisfies(m -> {
                assertThat(m.getName()).isEqualTo(APP_NAME);
            });
            assertThat(h).isInstanceOfSatisfying(BuildConfig.class, b -> {
                assertThat(b.getSpec().getOutput().getPushSecret()).isNull();
            });
        });
    }
}

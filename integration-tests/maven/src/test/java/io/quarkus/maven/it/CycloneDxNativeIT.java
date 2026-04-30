package io.quarkus.maven.it;

import static io.quarkus.maven.it.CycloneDxTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@EnableForNative
public class CycloneDxNativeIT extends MojoTestBase {

    @Test
    public void testNativeImage() throws Exception {
        final File testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-native");
        final RunningInvoker running = new RunningInvoker(testDir, false);

        final List<String> mvnArgs = TestUtils.nativeArguments("package", "-DskipTests", "-Dnative");
        final MavenProcessInvocationResult result = running.execute(mvnArgs, Collections.emptyMap());
        await().atMost(10, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        final String processLog = running.log();
        try {
            assertThat(processLog).containsIgnoringCase("BUILD SUCCESS");
        } catch (AssertionError ae) {
            Assumptions.assumeFalse(processLog.contains("Cannot find the `native-image"),
                    "Skipping test since native-image tool isn't available");
            throw ae;
        } finally {
            running.stop();
        }

        final Bom bom = parseSbom(testDir, "acme-app-1.0-SNAPSHOT-runner-cyclonedx.json");

        // native image main component is a generic file component (no Maven coords)
        final Component mainComponent = bom.getMetadata().getComponent();
        assertThat(mainComponent).isNotNull();
        assertThat(mainComponent.getName()).isEqualTo("acme-app-1.0-SNAPSHOT-runner");
        assertThat(mainComponent.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(mainComponent.getType()).isEqualTo(Component.Type.APPLICATION);
        assertThat(mainComponent.getPurl()).isEqualTo("pkg:generic/acme-app-1.0-SNAPSHOT-runner@1.0-SNAPSHOT");

        final List<Component> components = bom.getComponents();
        assertThat(components).isNotEmpty();
        assertComponent(components, "io.quarkus", "quarkus-rest", "runtime", null);
        assertComponent(components, "io.quarkus", "quarkus-rest-deployment", "development", null);
        assertComponent(components, "io.quarkus", "quarkus-cyclonedx", "runtime", null);
        assertComponent(components, "io.quarkus", "quarkus-cyclonedx-deployment", "development", null);
    }
}

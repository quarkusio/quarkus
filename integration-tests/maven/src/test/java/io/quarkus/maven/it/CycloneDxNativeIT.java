package io.quarkus.maven.it;

import static io.quarkus.maven.it.CycloneDxTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.parsers.JsonParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@EnableForNative
public class CycloneDxNativeIT extends MojoTestBase {

    @Test
    public void testNativeImage() throws Exception {
        final File testDir = initProject("projects/cyclonedx-sbom", "projects/cyclonedx-sbom-native");
        final RunningInvoker running = new RunningInvoker(testDir, false);

        final List<String> mvnArgs = TestUtils.nativeArguments("package", "-DskipTests", "-Dnative",
                "-Dquarkus.cyclonedx.embedded.enabled=true");
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

        verifySbomWithSyft(testDir.toPath(), "acme-app-1.0-SNAPSHOT-runner");
    }

    /**
     * Uses the anchore/syft container image to extract the SBOM embedded in the
     * native executable and verifies it contains the expected components.
     */
    private void verifySbomWithSyft(Path projectDir, String nativeExecutableName) throws Exception {
        final ContainerRuntime containerRuntime = ContainerRuntimeUtil.detectContainerRuntime(false);
        Assumptions.assumeTrue(containerRuntime != ContainerRuntime.UNAVAILABLE,
                "Skipping syft verification since no container runtime is available");

        final Path nativeImage = projectDir.resolve("target").resolve(nativeExecutableName);
        assertThat(nativeImage.toFile()).exists();

        final String runtime = containerRuntime.getExecutableName();
        final ProcessBuilder pb = new ProcessBuilder(
                runtime, "run", "--rm",
                "-v", nativeImage.toAbsolutePath() + ":/binary:ro,z",
                "anchore/syft",
                "/binary", "-o", "cyclonedx-json");
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);

        final Process process = pb.start();
        final String output;
        try (InputStream is = process.getInputStream()) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);
            output = baos.toString(StandardCharsets.UTF_8);
        }
        final int exitCode = process.waitFor();
        assertThat(exitCode)
                .as("syft exited with code %d", exitCode)
                .isZero();

        final Bom syftBom = new JsonParser().parse(output.getBytes(StandardCharsets.UTF_8));
        assertThat(syftBom).isNotNull();
        final List<Component> syftComponents = syftBom.getComponents();
        assertThat(syftComponents).isNotEmpty();

        assertThat(syftComponents.stream()
                .filter(c -> "io.quarkus".equals(c.getGroup()) && "quarkus-rest".equals(c.getName()))
                .findFirst())
                .as("syft-extracted SBOM should contain quarkus-rest")
                .isPresent();
        assertThat(syftComponents.stream()
                .filter(c -> "io.quarkus".equals(c.getGroup()) && "quarkus-cyclonedx".equals(c.getName()))
                .findFirst())
                .as("syft-extracted SBOM should contain quarkus-cyclonedx")
                .isPresent();
    }
}

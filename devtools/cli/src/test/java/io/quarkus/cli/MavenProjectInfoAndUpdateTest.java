package io.quarkus.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import picocli.CommandLine;

public class MavenProjectInfoAndUpdateTest extends RegistryClientBuilderTestBase {

    @BeforeAll
    static void configureRegistryAndMavenRepo() {
        TestRegistryClientBuilder.newInstance()
                .baseDir(registryConfigDir())
                .newRegistry("registry.acme.org")
                .newPlatform("org.acme.quarkus.platform")
                .newStream("2.0")
                .newRelease("2.0.0")
                .quarkusVersion(getCurrentQuarkusVersion())
                .addCoreMember()
                .alignPluginsOnQuarkusVersion()
                .addDefaultCodestartExtensions()
                .release()
                .newMember("acme-bom").addExtension("acme-quarkus-supersonic").addExtension("acme-quarkus-subatomic")
                .release().stream().platform()
                .newStream("1.0")
                .newRelease("1.0.0")
                .quarkusVersion(getCurrentQuarkusVersion())
                .addCoreMember()
                .alignPluginsOnQuarkusVersion()
                .addDefaultCodestartExtensions()
                .release()
                .newMember("acme-bom").addExtension("acme-quarkus-supersonic").addExtension("acme-quarkus-subatomic")
                .registry()
                .newNonPlatformCatalog(getCurrentQuarkusVersion())
                .addExtension("org.acme", "acme-quarkiverse-extension", "1.0")
                .registry()
                .clientBuilder()
                .build();
    }

    @Test
    void testClean() throws Exception {

        final CliDriver.Result createResult = run(workDir(), "create", "acme-clean",
                "-x supersonic,acme-quarkiverse-extension");
        assertThat(createResult.exitCode).isEqualTo(CommandLine.ExitCode.OK)
                .as(() -> "Expected OK return code." + createResult);
        assertThat(createResult.stdout).contains("SUCCESS")
                .as(() -> "Expected confirmation that the project has been created." + createResult);

        final Path projectDir = workDir().resolve("acme-clean");
        final CliDriver.Result infoResult = run(projectDir, "info");

        assertQuarkusPlatformBoms(infoResult.stdout,
                ArtifactCoords.pom("org.acme.quarkus.platform", "quarkus-bom", "2.0.0"),
                ArtifactCoords.pom("org.acme.quarkus.platform", "acme-bom", "2.0.0"));
        assertPlatformBomExtensions(infoResult.stdout, ArtifactCoords.pom("org.acme.quarkus.platform", "quarkus-bom", "2.0.0"),
                ArtifactCoords.jar("io.quarkus", "quarkus-arc", null));
        assertPlatformBomExtensions(infoResult.stdout, ArtifactCoords.pom("org.acme.quarkus.platform", "acme-bom", "2.0.0"),
                ArtifactCoords.jar("org.acme.quarkus.platform", "acme-quarkus-supersonic", null));
        assertRegistryExtensions(infoResult.stdout, "registry.acme.org",
                ArtifactCoords.jar("org.acme", "acme-quarkiverse-extension", "1.0"));

        final CliDriver.Result updateResult = run(projectDir, "update");
        assertThat(updateResult.stdout).contains("[INFO] The project is up-to-date");
    }

    @Test
    void testMissalignedPlatformExtensionVersion() throws Exception {

        final CliDriver.Result createResult = run(workDir(), "create", "acme-misaligned-ext-version",
                "-x supersonic,acme-quarkiverse-extension,org.acme.quarkus.platform:acme-quarkus-subatomic:1.0.0");
        assertThat(createResult.exitCode).isEqualTo(CommandLine.ExitCode.OK)
                .as(() -> "Expected OK return code." + createResult);
        assertThat(createResult.stdout).contains("SUCCESS")
                .as(() -> "Expected confirmation that the project has been created." + createResult);

        Path projectDir = workDir().resolve("acme-misaligned-ext-version");
        final CliDriver.Result infoResult = run(projectDir, "info");

        assertQuarkusPlatformBoms(infoResult.stdout,
                ArtifactCoords.pom("org.acme.quarkus.platform", "quarkus-bom", "2.0.0"),
                ArtifactCoords.pom("org.acme.quarkus.platform", "acme-bom", "2.0.0"));
        assertPlatformBomExtensions(infoResult.stdout, ArtifactCoords.pom("org.acme.quarkus.platform", "quarkus-bom", "2.0.0"),
                ArtifactCoords.jar("io.quarkus", "quarkus-arc", null));
        assertPlatformBomExtensions(infoResult.stdout, ArtifactCoords.pom("org.acme.quarkus.platform", "acme-bom", "2.0.0"),
                ArtifactCoords.jar("org.acme.quarkus.platform", "acme-quarkus-supersonic", null),
                ArtifactCoords.jar("org.acme.quarkus.platform", "acme-quarkus-subatomic", "1.0.0 | misaligned"));
        assertRegistryExtensions(infoResult.stdout, "registry.acme.org",
                ArtifactCoords.jar("org.acme", "acme-quarkiverse-extension", "1.0"));

        final CliDriver.Result rectifyResult = run(projectDir, "update", "--rectify");
        assertThat(rectifyResult.stdout)
                .contains("[INFO] Update: org.acme.quarkus.platform:acme-quarkus-subatomic:1.0.0 -> remove version (managed)");

        final CliDriver.Result updateResult = run(projectDir, "update", "-Dquarkus.platform.version=1.0.0");
        assertQuarkusPlatformBomUpdates(updateResult.stdout,
                ArtifactCoords.pom("org.acme.quarkus.platform", "quarkus-bom", "1.0.0 -> 2.0.0"),
                ArtifactCoords.pom("org.acme.quarkus.platform", "acme-bom", "1.0.0 -> 2.0.0"));
    }

    private static void assertPlatformBomExtensions(String output, ArtifactCoords bom, ArtifactCoords... extensions) {
        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {
            writer.write("[INFO] Extensions from ");
            writer.write(bom.getGroupId());
            writer.write(":");
            writer.write(bom.getArtifactId());
            writer.write(":");
            writer.newLine();
            for (ArtifactCoords c : extensions) {
                writer.write("[INFO]   ");
                writer.write(c.getGroupId());
                writer.write(":");
                writer.write(c.getArtifactId());
                if (c.getVersion() != null) {
                    writer.write(":");
                    writer.write(c.getVersion());
                }
                writer.newLine();
            }
            writer.write("[INFO] ");
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compose output fragment", e);
        }
        assertThat(output).contains(buf.getBuffer().toString());
    }

    private static void assertRegistryExtensions(String output, String id, ArtifactCoords... extensions) {
        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {
            writer.write("[INFO] Extensions from ");
            writer.write(id);
            writer.write(":");
            writer.newLine();
            for (ArtifactCoords c : extensions) {
                writer.write("[INFO]   ");
                writer.write(c.getGroupId());
                writer.write(":");
                writer.write(c.getArtifactId());
                writer.write(":");
                writer.write(c.getVersion());
                writer.newLine();
            }
            writer.write("[INFO] ");
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compose output fragment", e);
        }
        assertThat(output).contains(buf.getBuffer().toString());
    }

    private static void assertQuarkusPlatformBoms(String output, ArtifactCoords... coords) {
        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {
            writer.write("[INFO] Quarkus platform BOMs:");
            writer.newLine();
            for (ArtifactCoords c : coords) {
                writer.write("[INFO]   ");
                writer.write(c.toCompactCoords());
                writer.newLine();
            }
            writer.write("[INFO] ");
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compose output fragment", e);
        }
        assertThat(output).contains(buf.getBuffer().toString());
    }

    private static void assertQuarkusPlatformBomUpdates(String output, ArtifactCoords... coords) {
        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {
            writer.write("[INFO] Recommended Quarkus platform BOM updates:");
            writer.newLine();
            for (ArtifactCoords c : coords) {
                writer.write("[INFO] Update: ");
                writer.write(c.toCompactCoords());
                writer.newLine();
            }
            writer.write("[INFO] ");
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compose output fragment", e);
        }
        assertThat(output).contains(buf.getBuffer().toString());
    }
}

package io.quarkus.cli;

import static io.quarkus.devtools.messagewriter.MessageIcons.UP_TO_DATE_ICON;
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

        final CliDriver.Result createResult = run(workDir(), "create", "app", "acme-clean",
                "-x supersonic,acme-quarkiverse-extension");
        assertThat(createResult.exitCode).isEqualTo(CommandLine.ExitCode.OK)
                .as(() -> "Expected OK return code." + createResult);
        assertThat(createResult.stdout).contains("SUCCESS")
                .as(() -> "Expected confirmation that the project has been created." + createResult);

        final Path projectDir = workDir().resolve("acme-clean");
        final CliDriver.Result infoResult = run(projectDir, "info");
        assertThat(infoResult.getExitCode()).isEqualTo(0);

        assertQuarkusPlatformBoms(infoResult.stdout,
                UP_TO_DATE_ICON.iconOrMessage() + "       org.acme.quarkus.platform:quarkus-bom:pom:2.0.0",
                UP_TO_DATE_ICON.iconOrMessage() + "       org.acme.quarkus.platform:acme-bom:pom:2.0.0");

        assertPlatformBomExtensions(infoResult.stdout, ArtifactCoords.pom("org.acme.quarkus.platform", "quarkus-bom", "2.0.0"),
                UP_TO_DATE_ICON.iconOrMessage() + "       io.quarkus:quarkus-arc");

        assertPlatformBomExtensions(infoResult.stdout, ArtifactCoords.pom("org.acme.quarkus.platform", "acme-bom", "2.0.0"),
                UP_TO_DATE_ICON.iconOrMessage() + "       org.acme.quarkus.platform:acme-quarkus-supersonic");

        assertRegistryExtensions(infoResult.stdout, "registry.acme.org",
                UP_TO_DATE_ICON.iconOrMessage() + "       org.acme:acme-quarkiverse-extension:1.0");

        final CliDriver.Result updateResult = run(projectDir, "update", "-N");
        assertThat(updateResult.getExitCode()).isEqualTo(0);
        assertThat(updateResult.stdout).contains("The project is up-to-date " + UP_TO_DATE_ICON.iconOrMessage());
    }

    @Test
    void testMisalignedPlatformExtensionVersion() throws Exception {

        final CliDriver.Result createResult = run(workDir(), "create", "app", "acme-misaligned-ext-version",
                "-x supersonic,acme-quarkiverse-extension,org.acme.quarkus.platform:acme-quarkus-subatomic:1.0.0");
        assertThat(createResult.exitCode).isEqualTo(CommandLine.ExitCode.OK)
                .as(() -> "Expected OK return code." + createResult);
        assertThat(createResult.stdout).contains("SUCCESS")
                .as(() -> "Expected confirmation that the project has been created." + createResult);

        Path projectDir = workDir().resolve("acme-misaligned-ext-version");
        final CliDriver.Result infoResult = run(projectDir, "info");

        assertQuarkusPlatformBoms(infoResult.stdout,
                UP_TO_DATE_ICON.iconOrMessage() + "       org.acme.quarkus.platform:quarkus-bom:pom:2.0.0",
                UP_TO_DATE_ICON.iconOrMessage() + "       org.acme.quarkus.platform:acme-bom:pom:2.0.0");
        assertPlatformBomExtensions(infoResult.stdout, ArtifactCoords.pom("org.acme.quarkus.platform", "quarkus-bom", "2.0.0"),
                UP_TO_DATE_ICON.iconOrMessage() + "       io.quarkus:quarkus-arc");
        assertPlatformBomExtensions(infoResult.stdout, ArtifactCoords.pom("org.acme.quarkus.platform", "acme-bom", "2.0.0"),
                UP_TO_DATE_ICON.iconOrMessage() + "       org.acme.quarkus.platform:acme-quarkus-supersonic",
                "-       org.acme.quarkus.platform:acme-quarkus-subatomic:[1.0.0 -> managed]");
        assertRegistryExtensions(infoResult.stdout, "registry.acme.org",
                UP_TO_DATE_ICON.iconOrMessage() + "       org.acme:acme-quarkiverse-extension:1.0");

        CliDriver.Result updateResult = run(projectDir, "update", "--platform-version=1.0.0", "--no-rewrite");
        assertThat(updateResult.getExitCode()).isEqualTo(0);

        assertThat(updateResult.stdout)
                .contains(
                        "-       org.acme.quarkus.platform:acme-quarkus-subatomic:[1.0.0 -> managed]");
        assertQuarkusPlatformBomUpdates(updateResult.stdout,
                ArtifactCoords.pom("org.acme.quarkus.platform", "quarkus-bom", "[2.0.0 -> 1.0.0]"),
                ArtifactCoords.pom("org.acme.quarkus.platform", "acme-bom", "[2.0.0 -> 1.0.0]"));

        updateResult = run(projectDir, "update", "-Dquarkus.platform.version=1.0.0", "--no-rewrite");
        assertThat(updateResult.getExitCode()).isEqualTo(0);
        assertQuarkusPlatformBomUpdates(updateResult.stdout,
                ArtifactCoords.pom("org.acme.quarkus.platform", "quarkus-bom", "[1.0.0 -> 2.0.0]"),
                ArtifactCoords.pom("org.acme.quarkus.platform", "acme-bom", "[1.0.0 -> 2.0.0]"));
    }

    private static void assertPlatformBomExtensions(String output, ArtifactCoords bom, String... extensions) {
        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {
            writer.write("[INFO] Extensions from ");
            writer.write(bom.getGroupId());
            writer.write(":");
            writer.write(bom.getArtifactId());
            writer.write(":");
            writer.newLine();
            for (String c : extensions) {
                writer.write("[INFO]  ");
                writer.write(c);
                writer.newLine();
            }
            writer.write("[INFO] ");
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compose output fragment", e);
        }
        assertThat(output).contains(buf.getBuffer().toString());
    }

    private static void assertRegistryExtensions(String output, String id, String... extensions) {
        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {
            writer.write("[INFO] Extensions from ");
            writer.write(id);
            writer.write(":");
            writer.newLine();
            for (String c : extensions) {
                writer.write("[INFO]  ");
                writer.write(c);
                writer.newLine();
            }
            writer.write("[INFO] ");
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compose output fragment", e);
        }
        assertThat(output).contains(buf.getBuffer().toString());
    }

    private static void assertQuarkusPlatformBoms(String output, String... coords) {
        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {
            writer.write("[INFO] Quarkus platform BOMs:");
            writer.newLine();
            for (String c : coords) {
                writer.write("[INFO]  ");
                writer.write(c);
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
            writer.write("[INFO] Suggested Quarkus platform BOM updates:");
            writer.newLine();
            for (ArtifactCoords c : coords) {
                writer.write("[INFO]  ~       ");
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

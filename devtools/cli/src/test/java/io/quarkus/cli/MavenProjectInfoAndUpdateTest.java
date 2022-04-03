package io.quarkus.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.registry.config.RegistriesConfigLocator;
import picocli.CommandLine;

public class MavenProjectInfoAndUpdateTest {

    private static Path workDir;
    private static Path settingsXml;
    private static Path testRepo;
    private static String prevConfigPath;
    private static String prevRegistryClient;

    @BeforeAll
    static void setup() throws Exception {
        workDir = Path.of(System.getProperty("user.dir")).resolve("target").resolve("test-work-dir");
        final Path registryConfigDir = workDir.resolve("registry");

        final BootstrapMavenContext mavenContext = new BootstrapMavenContext(
                BootstrapMavenContext.config().setWorkspaceDiscovery(false));

        TestRegistryClientBuilder.newInstance()
                .baseDir(registryConfigDir)
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

        prevConfigPath = System.setProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY,
                registryConfigDir.resolve("config.yaml").toString());
        prevRegistryClient = System.setProperty("quarkusRegistryClient", "true");
        QuarkusProjectHelper.reset();

        final Settings settings = getBaseMavenSettings(mavenContext.getUserSettings().toPath());

        Profile profile = new Profile();
        settings.addActiveProfile("qs-test-registry");
        profile.setId("qs-test-registry");

        Repository repo = configureRepo("original-local",
                Path.of(mavenContext.getLocalRepo()).toUri().toURL().toExternalForm());
        profile.addRepository(repo);
        profile.addPluginRepository(repo);

        settings.addProfile(profile);
        repo = configureRepo("qs-test-registry",
                TestRegistryClientBuilder.getMavenRepoDir(registryConfigDir).toUri().toURL().toExternalForm());
        profile.addRepository(repo);
        profile.addPluginRepository(repo);

        settingsXml = workDir.resolve("settings.xml");
        try (BufferedWriter writer = Files.newBufferedWriter(settingsXml)) {
            new DefaultSettingsWriter().write(writer, Collections.emptyMap(), settings);
        }
        testRepo = registryConfigDir.resolve("test-repo");
    }

    private static Repository configureRepo(String id, String url)
            throws MalformedURLException, BootstrapMavenException {
        final Repository repo = new Repository();
        repo.setId(id);
        repo.setLayout("default");
        repo.setUrl(url);
        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(true);
        policy.setChecksumPolicy("ignore");
        policy.setUpdatePolicy("never");
        repo.setReleases(policy);
        repo.setSnapshots(policy);
        return repo;
    }

    private static String getCurrentQuarkusVersion() {
        String v = System.getProperty("project.version");
        if (v == null) {
            throw new IllegalStateException("project.version property isn't available");
        }
        return v;
    }

    private static Settings getBaseMavenSettings(Path mavenSettings) throws IOException {
        if (Files.exists(mavenSettings)) {
            try (BufferedReader reader = Files.newBufferedReader(mavenSettings)) {
                return new DefaultSettingsReader().read(reader, Collections.emptyMap());
            }
        }
        return new Settings();
    }

    private static void resetProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    @AfterAll
    static void cleanup() throws Exception {
        //CliDriver.deleteDir(workDir);
        resetProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, prevConfigPath);
        resetProperty("quarkusRegistryClient", prevRegistryClient);
    }

    @Test
    void testClean() throws Exception {

        final CliDriver.Result createResult = execute(workDir, "create", "acme-clean",
                "-x supersonic,acme-quarkiverse-extension");
        assertThat(createResult.exitCode).isEqualTo(CommandLine.ExitCode.OK)
                .as(() -> "Expected OK return code." + createResult);
        assertThat(createResult.stdout).contains("SUCCESS")
                .as(() -> "Expected confirmation that the project has been created." + createResult);

        final Path projectDir = workDir.resolve("acme-clean");
        final CliDriver.Result infoResult = execute(projectDir, "info");

        assertQuarkusPlatformBoms(infoResult.stdout,
                GACTV.pom("org.acme.quarkus.platform", "quarkus-bom", "2.0.0"),
                GACTV.pom("org.acme.quarkus.platform", "acme-bom", "2.0.0"));
        assertPlatformBomExtensions(infoResult.stdout, GACTV.pom("org.acme.quarkus.platform", "quarkus-bom", "2.0.0"),
                GACTV.jar("io.quarkus", "quarkus-arc", null));
        assertPlatformBomExtensions(infoResult.stdout, GACTV.pom("org.acme.quarkus.platform", "acme-bom", "2.0.0"),
                GACTV.jar("org.acme.quarkus.platform", "acme-quarkus-supersonic", null));
        assertRegistryExtensions(infoResult.stdout, "registry.acme.org",
                GACTV.jar("org.acme", "acme-quarkiverse-extension", "1.0"));

        final CliDriver.Result updateResult = execute(projectDir, "update");
        assertThat(updateResult.stdout).contains("[INFO] The project is up-to-date");
    }

    @Test
    void testMissalignedPlatformExtensionVersion() throws Exception {

        final CliDriver.Result createResult = execute(workDir, "create", "acme-misaligned-ext-version",
                "-x supersonic,acme-quarkiverse-extension,org.acme.quarkus.platform:acme-quarkus-subatomic:1.0.0");
        assertThat(createResult.exitCode).isEqualTo(CommandLine.ExitCode.OK)
                .as(() -> "Expected OK return code." + createResult);
        assertThat(createResult.stdout).contains("SUCCESS")
                .as(() -> "Expected confirmation that the project has been created." + createResult);

        Path projectDir = workDir.resolve("acme-misaligned-ext-version");
        final CliDriver.Result infoResult = execute(projectDir, "info");

        assertQuarkusPlatformBoms(infoResult.stdout,
                GACTV.pom("org.acme.quarkus.platform", "quarkus-bom", "2.0.0"),
                GACTV.pom("org.acme.quarkus.platform", "acme-bom", "2.0.0"));
        assertPlatformBomExtensions(infoResult.stdout, GACTV.pom("org.acme.quarkus.platform", "quarkus-bom", "2.0.0"),
                GACTV.jar("io.quarkus", "quarkus-arc", null));
        assertPlatformBomExtensions(infoResult.stdout, GACTV.pom("org.acme.quarkus.platform", "acme-bom", "2.0.0"),
                GACTV.jar("org.acme.quarkus.platform", "acme-quarkus-supersonic", null),
                GACTV.jar("org.acme.quarkus.platform", "acme-quarkus-subatomic", "1.0.0 | misaligned"));
        assertRegistryExtensions(infoResult.stdout, "registry.acme.org",
                GACTV.jar("org.acme", "acme-quarkiverse-extension", "1.0"));

        final CliDriver.Result rectifyResult = execute(projectDir, "update", "--rectify");
        assertThat(rectifyResult.stdout)
                .contains("[INFO] Update: org.acme.quarkus.platform:acme-quarkus-subatomic:1.0.0 -> remove version (managed)");

        final CliDriver.Result updateResult = execute(projectDir, "update", "-Dquarkus.platform.version=1.0.0");
        assertQuarkusPlatformBomUpdates(updateResult.stdout,
                GACTV.pom("org.acme.quarkus.platform", "quarkus-bom", "1.0.0 -> 2.0.0"),
                GACTV.pom("org.acme.quarkus.platform", "acme-bom", "1.0.0 -> 2.0.0"));
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

    private CliDriver.Result execute(Path dir, String... args) throws Exception {
        return CliDriver.builder()
                .setStartingDir(dir)
                .setMavenRepoLocal(testRepo.toString())
                .setMavenSettings(settingsXml.toString())
                .addArgs(args)
                .execute();
    }
}

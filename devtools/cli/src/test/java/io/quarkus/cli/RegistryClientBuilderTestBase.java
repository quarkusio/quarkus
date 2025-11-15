package io.quarkus.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.registry.config.RegistriesConfigLocator;

public abstract class RegistryClientBuilderTestBase {

    private static Path workDir;
    private static Path settingsXml;
    private static Path testRepo;
    private static String prevConfigPath;
    private static String prevRegistryClient;

    static Path workDir() {
        if (workDir == null) {
            var p = Path.of(System.getProperty("user.dir")).resolve("target").resolve("test-classes").resolve("test-work-dir");
            try {
                Files.createDirectories(p);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create work dir " + p);
            }
            workDir = p;
        }
        return workDir;
    }

    static Path registryConfigDir() {
        return workDir().resolve("registry");
    }

    @BeforeAll
    static void setup() throws Exception {
        final Path registryConfigDir = registryConfigDir();

        prevConfigPath = System.setProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY,
                registryConfigDir.resolve("config.yaml").toString());
        prevRegistryClient = System.setProperty("quarkusRegistryClient", "true");
        QuarkusProjectHelper.reset();

        final BootstrapMavenContext mavenContext = new BootstrapMavenContext(
                BootstrapMavenContext.config().setWorkspaceDiscovery(false));
        final Settings settings = getBaseMavenSettings(mavenContext.getUserSettings());

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

        settingsXml = workDir().resolve("settings.xml");
        try (BufferedWriter writer = Files.newBufferedWriter(settingsXml)) {
            new DefaultSettingsWriter().write(writer, Map.of(), settings);
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

    protected static String getCurrentQuarkusVersion() {
        String v = System.getProperty("project.version");
        if (v == null) {
            throw new IllegalStateException("project.version property isn't available");
        }
        return v;
    }

    private static Settings getBaseMavenSettings(File mavenSettings) throws IOException {
        if (mavenSettings != null && mavenSettings.exists()) {
            return new DefaultSettingsReader().read(mavenSettings, Map.of());
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
        CliDriver.deleteDir(workDir);
        resetProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, prevConfigPath);
        resetProperty("quarkusRegistryClient", prevRegistryClient);
        workDir = null;
        settingsXml = null;
        testRepo = null;
    }

    protected CliDriver.Result run(Path dir, String... args) throws Exception {
        return CliDriver.builder()
                .setStartingDir(dir)
                .setMavenRepoLocal(testRepo.toString())
                .setMavenSettings(settingsXml.toString())
                .addArgs(args)
                .execute();
    }
}

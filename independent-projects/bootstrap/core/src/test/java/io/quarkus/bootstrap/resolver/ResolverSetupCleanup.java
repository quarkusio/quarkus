package io.quarkus.bootstrap.resolver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.settings.Activation;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolverSetupCleanup {

    protected Path workDir;
    private Path repoHome;
    private Path localRepoHome;
    private Path settingsXml;
    protected BootstrapAppModelResolver resolver;
    protected TsRepoBuilder repo;

    protected Map<String, String> originalProps;

    @BeforeEach
    public void setup() throws Exception {
        setSystemProperties();
        workDir = initWorkDir();
        repoHome = IoUtils.mkdirs(workDir.resolve("repo"));
        if (setupCustomMavenRepoInSettings()) {
            localRepoHome = IoUtils.mkdirs(workDir).resolve("local-repo");

            final Settings settings = new Settings();
            final Profile profile = new Profile();
            final Activation activation = new Activation();
            activation.setActiveByDefault(true);
            profile.setActivation(activation);
            final Repository repo = new Repository();
            repo.setId("custom-repo");
            repo.setName("Custom Test Repo");
            repo.setLayout("default");
            try {
                repo.setUrl(repoHome.toUri().toURL().toExternalForm());
            } catch (MalformedURLException e) {
                throw new BootstrapMavenException("Failed to initialize Maven repo URL", e);
            }
            RepositoryPolicy policy = new RepositoryPolicy();
            policy.setEnabled(true);
            policy.setChecksumPolicy("ignore");
            policy.setUpdatePolicy("never");
            repo.setReleases(policy);
            repo.setSnapshots(policy);
            profile.setId("custom-repo");
            profile.addRepository(repo);
            settings.addProfile(profile);

            settingsXml = workDir.resolve("settings.xml");
            try (BufferedWriter writer = Files.newBufferedWriter(settingsXml)) {
                new DefaultSettingsWriter().write(writer, Map.of(), settings);
            } catch (IOException e) {
                throw new BootstrapMavenException("Failed to persist settings.xml", e);
            }
        } else {
            localRepoHome = repoHome;
        }

        resolver = newAppModelResolver(null);
        repo = TsRepoBuilder.getInstance(newArtifactResolver(null, true), workDir);
    }

    @AfterEach
    public void cleanup() {
        if (cleanWorkDir() && workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
        if (originalProps != null) {
            for (Map.Entry<String, String> prop : originalProps.entrySet()) {
                if (prop.getValue() == null) {
                    System.clearProperty(prop.getKey());
                } else {
                    System.setProperty(prop.getKey(), prop.getValue());
                }
            }
            originalProps = null;
        }
    }

    protected Path getInstallDir() {
        return repoHome;
    }

    /**
     * Enabling this option will install all the artifacts to a Maven repo that
     * will be enabled in the Maven settings as a remote repo for the test.
     * Otherwise, all the artifacts will be installed in a Maven repo that will
     * be configured as a local repo for the test.
     *
     * @return whether to setup a custom remote Maven repo for the test
     */
    protected boolean setupCustomMavenRepoInSettings() {
        return false;
    }

    protected void setSystemProperties() {
    }

    protected void setSystemProperty(String name, String value) {
        if (originalProps == null) {
            originalProps = new HashMap<>();
        }
        final String prevValue = System.setProperty(name, value);
        if (!originalProps.containsKey(name)) {
            originalProps.put(name, prevValue);
        }
    }

    protected Path initWorkDir() {
        return IoUtils.createRandomTmpDir();
    }

    protected Path getSettingsXml() {
        return settingsXml;
    }

    protected Path getLocalRepoHome() {
        return localRepoHome;
    }

    protected boolean cleanWorkDir() {
        return true;
    }

    protected QuarkusBootstrap.Mode getBootstrapMode() {
        return QuarkusBootstrap.Mode.PROD;
    }

    protected BootstrapAppModelResolver newAppModelResolver(LocalProject currentProject) throws Exception {
        final BootstrapAppModelResolver appModelResolver = new BootstrapAppModelResolver(newArtifactResolver(currentProject));
        appModelResolver.setLegacyModelResolver(BootstrapAppModelResolver.isLegacyModelResolver(null));
        switch (getBootstrapMode()) {
            case PROD:
                break;
            case TEST:
                appModelResolver.setTest(true);
                break;
            case DEV:
                appModelResolver.setDevMode(true);
                break;
            default:
                throw new IllegalArgumentException("Not supported bootstrap mode " + getBootstrapMode());
        }
        return appModelResolver;
    }

    protected MavenArtifactResolver newArtifactResolver(LocalProject currentProject) throws BootstrapMavenException {
        return newArtifactResolver(currentProject, false);
    }

    private MavenArtifactResolver newArtifactResolver(LocalProject currentProject, boolean forInstalling)
            throws BootstrapMavenException {
        final MavenArtifactResolver.Builder builder = MavenArtifactResolver.builder()
                .setOffline(true)
                .setWorkspaceDiscovery(false)
                .setCurrentProject(currentProject);
        if (forInstalling) {
            builder.setLocalRepository(repoHome.toString());
        } else {
            builder.setLocalRepository(localRepoHome.toString());
            if (settingsXml != null) {
                builder.setUserSettings(settingsXml.toFile()).setOffline(false);
            }
        }
        return builder.build();
    }

    protected TsQuarkusExt install(TsQuarkusExt extension) {
        extension.install(repo);
        return extension;
    }

    protected TsArtifact install(TsArtifact artifact) {
        repo.install(artifact);
        return artifact;
    }

    protected TsArtifact install(TsArtifact artifact, Path p) {
        repo.install(artifact, p);
        return artifact;
    }
}

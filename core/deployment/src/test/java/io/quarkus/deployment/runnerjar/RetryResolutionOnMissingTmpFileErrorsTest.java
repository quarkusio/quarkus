package io.quarkus.deployment.runnerjar;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.collection.CollectRequest;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;

public class RetryResolutionOnMissingTmpFileErrorsTest extends BootstrapFromOriginalJarTestBase {

    private static final String QUARKUS_VERSION = System.getProperty("project.version");

    private static class TempFileCleaner implements Runnable {

        private final Path targetRepositoryFile;

        private TempFileCleaner(Path targetRepositoryFile) {
            this.targetRepositoryFile = targetRepositoryFile;
        }

        @Override
        public void run() {
            final String targetFileName = targetRepositoryFile.getFileName().toString();
            final Path artifactDir = targetRepositoryFile.getParent();
            boolean terminate = false;
            while (!terminate) {
                if (Files.exists(artifactDir)) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(artifactDir)) {
                        for (var p : stream) {
                            final String fileName = p.getFileName().toString();
                            if (fileName.equals(targetFileName)) {
                                terminate = true;
                            } else if (fileName.endsWith(".tmp") || fileName.endsWith(".part")) {
                                // delete in-progress files to fail artifact resolution
                                if (Files.deleteIfExists(p)) {
                                    System.out.println("Deleted " + p.getFileName());
                                    terminate = true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //                try {
                //                    Thread.sleep(50);
                //                } catch (InterruptedException e) {
                //                }
                if (!terminate) {
                    terminate = Files.exists(targetRepositoryFile);
                }
            }
        }
    }

    final AtomicBoolean sawFileMissingErrors = new AtomicBoolean(false);

    @Override
    protected MavenArtifactResolver newArtifactResolver(LocalProject currentProject) throws BootstrapMavenException {

        /**
         * This is the tricky part.
         * Here we initialize a Maven artifact resolver to be used by Quarkus to resolve the ApplicationModel.
         *
         * First, it will capture events indicating that certain artifacts will be resolved. For non-SNAPSHOT
         * artifacts, we register tasks that will be scanning those artifact directories in the local Maven repository
         * trying to capture temporary (in-progress) files that the "Maven resolver" writes artifact content to
         * before renaming them to the final artifact files.
         * Once those in-progress files are detected, they are removed. This will cause Quarkus deployment dependency
         * CollectRequest to fail, which the {@link io.quarkus.bootstrap.resolver.maven.ApplicationDependencyResolver}
         * is expected to re-try.
         *
         * This is done exclusively for deployment dependencies, since this is what is parallelized
         * in the {@link io.quarkus.bootstrap.resolver.maven.ApplicationDependencyResolver}.
         *
         * The test uses a custom location for a local Maven repository. To be able to resolve locally built Quarkus artifacts,
         * we create custom settings an active profile where the current local repository is added as remote repository.
         */

        var originalMvnCtx = new BootstrapMavenContext((BootstrapMavenContext.config().setWorkspaceDiscovery(false)));
        final Settings settings = originalMvnCtx.getEffectiveSettings();
        final String originalLocalRepo = originalMvnCtx.getLocalRepo();

        Path settingsXml;
        try {
            Profile profile = new Profile();
            settings.addActiveProfile("original-local");
            profile.setId("original-local");

            final Repository repo = new Repository();
            repo.setId("original-local");
            repo.setLayout("default");
            repo.setUrl(Path.of(originalLocalRepo).toUri().toURL().toExternalForm());
            RepositoryPolicy releases = new RepositoryPolicy();
            releases.setEnabled(false);
            releases.setChecksumPolicy("ignore");
            releases.setUpdatePolicy("never");
            repo.setReleases(releases);
            RepositoryPolicy snapshots = new RepositoryPolicy();
            snapshots.setEnabled(true);
            snapshots.setChecksumPolicy("ignore");
            snapshots.setUpdatePolicy("never");
            repo.setSnapshots(snapshots);

            profile.addRepository(repo);
            profile.addPluginRepository(repo);

            settings.addProfile(profile);

            settingsXml = workDir.resolve("settings.xml");
            try (BufferedWriter writer = Files.newBufferedWriter(settingsXml)) {
                new DefaultSettingsWriter().write(writer, Map.of(), settings);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test settings.xml", e);
        }

        var ctx = new BootstrapMavenContext(BootstrapMavenContext.config()
                // A bit of a peculiar detail, there appears to be a difference between running`mvn test`
                // and `mvn test -Dtest=RetryResolutionOnTmpFilesMissingTest`.
                // Unless the current project is set to the module directory, the test belongs to, `mvn test` will fail
                // while `mvn test -Dtest=RetryResolutionOnTmpFilesMissingTest` will not.
                .setCurrentProject(Path.of("").normalize().toAbsolutePath().toString())
                .setWorkspaceDiscovery(true)
                .setArtifactTransferLogging(false)
                .setUserSettings(settingsXml.toFile())
                .setLocalRepository(getLocalRepoHome().toString()));

        var session = new DefaultRepositorySystemSession(ctx.getRepositorySystemSession());
        final Map<ArtifactCoords, CompletableFuture<?>> tmpFileCleaners = new ConcurrentHashMap<>();
        final Set<String> ignoredGroupIds = Set.of("org.acme", "io.quarkus.bootstrap.test");
        session.setRepositoryListener(new AbstractRepositoryListener() {
            @Override
            public void artifactResolving(RepositoryEvent event) {
                // we are looking exclusively for deployment dependency requests
                if (!isPartOfCollectingDeploymentDeps(event)) {
                    return;
                }
                var a = event.getArtifact();
                // delete non-snapshot artifacts to trigger re-download but only once
                if (!ignoredGroupIds.contains(a.getGroupId()) && !a.isSnapshot()) {
                    tmpFileCleaners.computeIfAbsent(
                            DependencyUtils.getCoords(a),
                            k -> {
                                final Path targetRepositoryFile = event.getSession().getLocalRepository().getBasedir().toPath()
                                        .resolve(event.getSession().getLocalRepositoryManager().getPathForLocalArtifact(a));
                                try {
                                    if (Files.deleteIfExists(targetRepositoryFile)) {
                                        System.out.println("To be (re)resolved " + event.getArtifact());
                                    }
                                } catch (IOException e) {
                                }
                                return CompletableFuture.runAsync(new TempFileCleaner(targetRepositoryFile));
                            });
                }
            }

            @Override
            public void artifactResolved(RepositoryEvent event) {
                // just to catch the fact that some requests failed due to missing files
                if (!sawFileMissingErrors.get() && !event.getExceptions().isEmpty()) {
                    for (var e : event.getExceptions()) {
                        if (isCausedByMissingFile(e)) {
                            sawFileMissingErrors.set(true);
                            break;
                        }
                    }
                }
            }

            @Override
            public void artifactDownloading(RepositoryEvent event) {
                final ArtifactCoords coords = DependencyUtils.getCoords(event.getArtifact());
                StringBuilder sb = new StringBuilder();
                sb.append(tmpFileCleaners.containsKey(coords) ? "Re-downloading" : "Downloading")
                        .append(" from ").append(event.getRepository().getId()).append(": ").append(coords.toCompactCoords());
                System.out.println(sb);
            }
        });

        return MavenArtifactResolver.builder()
                .setRepositorySystem(ctx.getRepositorySystem())
                .setRepositorySystemSession(session)
                .setRemoteRepositories(ctx.getRemoteRepositories())
                .setRemoteRepositoryManager(ctx.getRemoteRepositoryManager())
                .setSettingsDecrypter(ctx.getSettingsDecrypter())
                .build();
    }

    @Override
    protected TsArtifact composeApplication() {
        assertThat(QUARKUS_VERSION).isNotNull();
        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(TsArtifact.jar("io.quarkus", "quarkus-core", QUARKUS_VERSION));
    }

    @Override
    protected void assertAppModel(ApplicationModel model) throws Exception {
        // make sure there were resolution failures related to missing files
        assertThat(sawFileMissingErrors).isTrue();

        // basic model assertion
        Set<ArtifactCoords> topCoords = new HashSet<>(1);
        for (var topExt : model.getDependencies(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT)) {
            topCoords.add(ArtifactCoords.of(topExt.getGroupId(), topExt.getArtifactId(), topExt.getClassifier(),
                    topExt.getType(), topExt.getVersion()));
        }
        assertThat(topCoords).containsExactly(ArtifactCoords.jar("io.quarkus", "quarkus-core", QUARKUS_VERSION));
    }

    @Override
    protected void assertLibDirectoryContent(Set<String> actualMainLib) {
        // skipping, since it involves iterating over quarkus-core dependencies
    }

    private static boolean isCausedByMissingFile(Exception e) {
        Throwable t = e;
        while (t != null) {
            // It looks like in Maven 3.9 it's NoSuchFileException, while in Maven 3.8 it's FileNotFoundException
            if (t instanceof NoSuchFileException || t instanceof FileNotFoundException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isPartOfCollectingDeploymentDeps(RepositoryEvent event) {
        RequestTrace requestTrace = getRootRequest(event);
        if (requestTrace.getData() instanceof CollectRequest cr) {
            if (cr.getRootArtifact().getArtifactId().endsWith("-deployment")) {
                return true;
            }
        }
        ;
        return false;
    }

    private static RequestTrace getRootRequest(RepositoryEvent event) {
        RequestTrace requestTrace = event.getTrace();
        RequestTrace parent;
        while ((parent = requestTrace.getParent()) != null) {
            requestTrace = parent;
        }
        return requestTrace;
    }
}

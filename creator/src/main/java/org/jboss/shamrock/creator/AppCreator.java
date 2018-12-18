/**
 *
 */
package org.jboss.shamrock.creator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.jboss.shamrock.creator.resolver.aether.AetherArtifactResolver;
import org.jboss.shamrock.creator.resolver.aether.MavenRepoInitializer;
import org.jboss.shamrock.creator.resolver.aether.AppCreatorDependencySelector;
import org.jboss.shamrock.creator.resolver.aether.AppCreatorLocalRepositoryManager;
import org.jboss.shamrock.creator.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class AppCreator {

    private AppArtifactResolver artifactResolver;
    private List<AppCreationPhase> phases = new ArrayList<>(1);
    private boolean debug;
    private Path workDir;
    private Map<Class<? extends AppCreationPhaseOutcome>, AppCreationPhaseOutcome> phaseOutcomes;

    /**
     * Artifact resolver which should be used to resolve application
     * dependencies.
     * If artifact resolver is not set by the user, the default one will be
     * created based on the user Maven settings.xml file.
     *
     * @param resolver  artifact resolver
     * @return  this AppCreator instance
     */
    public AppCreator setArtifactResolver(AppArtifactResolver resolver) {
        this.artifactResolver = resolver;
        return this;
    }

    /**
     * Adds a creation phase to the application creation flow.
     * In the current implementation the phases are processed in the order
     * they are added.
     *
     * @param phase  application creation phase
     * @return  this AppCreator instance
     */
    public AppCreator addPhase(AppCreationPhase phase) {
        phases.add(phase);
        return this;
    }

    public AppCreator setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    /**
     * Work directory used by the phases to store various data.
     * If it's not set by the user, a temporary directory will be created
     * which also be automatically removed after the application have passed
     * through all the creation phases.
     *
     * @param p  work directory
     * @return  this AppCreator instance
     */
    public AppCreator setWorkDir(Path p) {
        this.workDir = p;
        return this;
    }

    /**
     * This method allows to push an already available outcome for a certain
     * phase before the build process has actually started.
     *
     * @param type  type of the outcome
     * @param outcome  phase outcome
     * @throws AppCreatorException  in case the outcome couldn't be accepted
     */
    public <O extends AppCreationPhaseOutcome> AppCreator pushOutcome(Class<O> type, O outcome) throws AppCreatorException {
        if(phaseOutcomes == null) {
            phaseOutcomes = new HashMap<>(1);
        }
        if(phaseOutcomes.put(type, outcome) != null) {
            // let's for now be strict about it
            throw new AppCreatorException("Phase outcome of type " + type.getName() + " has already been provided");
        }
        return this;
    }

    /**
     * Initiates an application creation process for an application JAR.
     *
     * @param appJar  application JAR
     * @throws AppCreatorException  in case of a failure
     */
    public void create(Path appJar) throws AppCreatorException {
        final Properties props = new Properties();
        try (FileSystem fs = FileSystems.newFileSystem(appJar, null)) {
            final Path metaInfMaven = fs.getPath("META-INF", "maven");
            Path pomProps = null;
            if (Files.exists(metaInfMaven)) {
                try (DirectoryStream<Path> groupIds = Files.newDirectoryStream(metaInfMaven)) {
                    for (Path groupId : groupIds) {
                        if (!Files.isDirectory(groupId)) {
                            continue;
                        }
                        try (DirectoryStream<Path> artifactIds = Files.newDirectoryStream(groupId)) {
                            for (Path artifactId : artifactIds) {
                                if (!Files.isDirectory(artifactId)) {
                                    continue;
                                }
                                final Path tmp = artifactId.resolve("pom.properties");
                                if (Files.exists(tmp)) {
                                    pomProps = tmp;
                                    break;
                                }
                            }
                        }
                        if (pomProps != null) {
                            break;
                        }
                    }
                }
            }
            if(pomProps == null) {
                throw new AppCreatorException("Failed to located META-INF/maven/<groupId>/<artifactId>/pom.properties in " + appJar);
            }
            try (InputStream is = Files.newInputStream(pomProps)) {
                props.load(is);
            }
        } catch (IOException e) {
            throw new AppCreatorException("Failed to load pom.properties from " + appJar, e);
        }

        final AppArtifact appArtifact = new AppArtifact(props.getProperty("groupId"), props.getProperty("artifactId"), props.getProperty("version"));

        Path tmpDir = null;
        AppArtifactResolver artifactResolver = this.artifactResolver;
        if(artifactResolver == null) {
            tmpDir = workDir == null ? IoUtils.createRandomTmpDir() : workDir;
            artifactResolver = getDefaultArtifactResolver(tmpDir.resolve("repo"));
        }
        artifactResolver.relink(appArtifact, appJar);

        create(appArtifact, artifactResolver, tmpDir, workDir == null);
    }

    /**
     * Initiates an application creation process for an application artifact
     * coordinates.
     *
     * @param appArtifact  application artifact coordinates
     * @throws AppCreatorException
     */
    public void create(AppArtifact appArtifact) throws AppCreatorException {
        Path tmpDir = null;
        AppArtifactResolver artifactResolver = this.artifactResolver;
        if(artifactResolver == null) {
            tmpDir = workDir == null ? IoUtils.createRandomTmpDir() : workDir;
            artifactResolver = getDefaultArtifactResolver(tmpDir.resolve("repo"));
        }
        create(appArtifact, artifactResolver, tmpDir, workDir == null);
    }

    private void create(AppArtifact appArtifact, AppArtifactResolver artifactResolver, Path workDir, boolean deleteWorkDir) throws AppCreatorException {
        try (AppCreationContext ctx = new AppCreationContext(appArtifact, artifactResolver)) {
            ctx.setWorkDir(workDir);
            if(phaseOutcomes != null) {
                for(Map.Entry<Class<? extends AppCreationPhaseOutcome>, AppCreationPhaseOutcome> entry : phaseOutcomes.entrySet()) {
                    ctx.protectedPushOutcome(entry.getKey(), entry.getValue());
                }
            }
            for (AppCreationPhase phase : phases) {
                phase.process(ctx);
            }
        } finally {
            if(deleteWorkDir) {
                IoUtils.recursiveDelete(workDir);
            }
        }
    }

    private AppArtifactResolver getDefaultArtifactResolver(Path repoHome) throws AppCreatorException {
        final RepositorySystem repoSystem = MavenRepoInitializer.getRepositorySystem();
        final Settings settings = MavenRepoInitializer.getSettings();
        final DefaultRepositorySystemSession repoSession = MavenRepoInitializer.newSession(repoSystem, settings);
        final AppCreatorLocalRepositoryManager appCreatorLocalRepoManager = new AppCreatorLocalRepositoryManager(repoSystem.newLocalRepositoryManager(repoSession,
                new LocalRepository(repoHome.toString())), Paths.get(MavenRepoInitializer.getLocalRepo(settings)));
        repoSession.setLocalRepositoryManager(appCreatorLocalRepoManager);
        repoSession.setDependencySelector(new AppCreatorDependencySelector(debug));
        final AetherArtifactResolver resolver = new AetherArtifactResolver(repoSystem, repoSession, MavenRepoInitializer.getRemoteRepos(settings));
        resolver.setLocalRepositoryManager(appCreatorLocalRepoManager);
        return resolver;
    }
}

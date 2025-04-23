package io.quarkus.bootstrap;

import static io.quarkus.bootstrap.util.BootstrapUtils.readAppModelWithWorkspaceId;
import static io.quarkus.bootstrap.util.BootstrapUtils.writeAppModelWithWorkspaceId;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CurationResult;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContextConfig;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * The factory that creates the application dependency model.
 *
 * This is used to build the application class loader.
 */
public class BootstrapAppModelFactory {

    private static final String QUARKUS = "quarkus";
    private static final String BOOTSTRAP = "bootstrap";
    private static final String APP_MODEL_DAT = "app-model.dat";

    public static final String CREATOR_APP_GROUP_ID = "creator.app.groupId";
    public static final String CREATOR_APP_ARTIFACT_ID = "creator.app.artifactId";
    public static final String CREATOR_APP_CLASSIFIER = "creator.app.classifier";
    public static final String CREATOR_APP_TYPE = "creator.app.type";
    public static final String CREATOR_APP_VERSION = "creator.app.version";

    private static final Logger log = Logger.getLogger(BootstrapAppModelFactory.class);

    public static BootstrapAppModelFactory newInstance() {
        return new BootstrapAppModelFactory();
    }

    private ArtifactCoords managingProject;
    private Path projectRoot;
    private List<Path> appCp = new ArrayList<>(0);
    private Boolean localProjectsDiscovery;
    private Boolean offline;
    private boolean enableClasspathCache;
    private boolean test;
    private boolean devMode;
    private AppModelResolver bootstrapAppModelResolver;

    private ResolvedDependency appArtifact;
    private MavenArtifactResolver mavenArtifactResolver;

    private BootstrapMavenContext mvnContext;
    Set<ArtifactKey> reloadableModules = Set.of();

    private Collection<io.quarkus.maven.dependency.Dependency> forcedDependencies = List.of();

    private BootstrapAppModelFactory() {
    }

    public BootstrapAppModelFactory setTest(boolean test) {
        this.test = test;
        return this;
    }

    public BootstrapAppModelFactory setDevMode(boolean devMode) {
        this.devMode = devMode;
        return this;
    }

    public BootstrapAppModelFactory setLocalArtifacts(Set<ArtifactKey> localArtifacts) {
        this.reloadableModules = new HashSet<>(localArtifacts);
        return this;
    }

    public BootstrapAppModelFactory setProjectRoot(Path projectRoot) {
        this.projectRoot = projectRoot;
        return this;
    }

    public BootstrapAppModelFactory addToClassPath(Path path) {
        this.appCp.add(path);
        return this;
    }

    public BootstrapAppModelFactory setLocalProjectsDiscovery(Boolean localProjectsDiscovery) {
        this.localProjectsDiscovery = localProjectsDiscovery;
        return this;
    }

    public BootstrapAppModelFactory setOffline(Boolean offline) {
        this.offline = offline;
        return this;
    }

    public BootstrapAppModelFactory setEnableClasspathCache(boolean enable) {
        this.enableClasspathCache = enable;
        return this;
    }

    public BootstrapAppModelFactory setBootstrapAppModelResolver(AppModelResolver bootstrapAppModelResolver) {
        this.bootstrapAppModelResolver = bootstrapAppModelResolver;
        return this;
    }

    public BootstrapAppModelFactory setAppArtifact(ResolvedDependency appArtifact) {
        this.appArtifact = appArtifact;
        return this;
    }

    public BootstrapAppModelFactory setForcedDependencies(
            Collection<io.quarkus.maven.dependency.Dependency> forcedDependencies) {
        this.forcedDependencies = forcedDependencies;
        return this;
    }

    public AppModelResolver getAppModelResolver() {
        if (bootstrapAppModelResolver != null) {
            return bootstrapAppModelResolver;
        }

        try {
            if (projectRoot != null && !Files.isDirectory(projectRoot)) {
                final MavenArtifactResolver mvn;
                if (mavenArtifactResolver == null) {
                    final BootstrapMavenContext mvnCtx = createBootstrapMavenContext();
                    if (managingProject == null) {
                        managingProject = mvnCtx.getCurrentProjectArtifact(ArtifactCoords.TYPE_POM);
                    }
                    mvn = new MavenArtifactResolver(mvnCtx);
                } else {
                    mvn = mavenArtifactResolver;
                }
                return bootstrapAppModelResolver = initAppModelResolver(mvn);
            }

            MavenArtifactResolver mvn = mavenArtifactResolver;
            if (mvn == null) {
                mvn = new MavenArtifactResolver(createBootstrapMavenContext());
            }
            return bootstrapAppModelResolver = initAppModelResolver(mvn);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create application model resolver for " + projectRoot, e);
        }
    }

    private BootstrapAppModelResolver initAppModelResolver(MavenArtifactResolver artifactResolver) {
        var appModelResolver = new BootstrapAppModelResolver(artifactResolver)
                .setTest(test)
                .setDevMode(devMode);
        var project = artifactResolver.getMavenContext().getCurrentProject();
        if (project != null) {
            final Properties modelProps = project.getEffectiveModel() == null
                    ? project.getRawModel().getProperties()
                    : project.getEffectiveModel().getProperties();
            appModelResolver.setLegacyModelResolver(BootstrapAppModelResolver.isLegacyModelResolver(modelProps));
        }
        return appModelResolver;
    }

    private BootstrapMavenContext createBootstrapMavenContext() throws AppModelResolverException {
        if (mvnContext != null) {
            return mvnContext;
        }
        if (mavenArtifactResolver != null) {
            mvnContext = mavenArtifactResolver.getMavenContext();
            if (mvnContext != null) {
                return mvnContext;
            }
        }
        final BootstrapMavenContextConfig<?> config = BootstrapMavenContext.config();
        if (offline != null) {
            config.setOffline(offline);
        }
        // Currently projectRoot may be an app location which is not exactly a Maven project dir
        final Path projectPom = config.getPomForDirOrNull(projectRoot);
        if (projectPom != null) {
            config.setCurrentProject(projectPom.toString());
        }
        config.setWorkspaceDiscovery(isWorkspaceDiscoveryEnabled());
        return mvnContext = new BootstrapMavenContext(config);
    }

    public CurationResult resolveAppModel() throws BootstrapException {
        CurationResult result = loadFromSystemProperty();
        if (result != null) {
            return result;
        }

        result = createAppModelForJarOrNull(projectRoot);
        if (result != null) {
            return result;
        }

        return resolveAppModelForWorkspace();
    }

    /**
     * Resolves an application for a project in a workspace.
     *
     * @return application model
     * @throws BootstrapException in case of a failure
     */
    private CurationResult resolveAppModelForWorkspace() throws BootstrapException {
        ResolvedDependency appArtifact = this.appArtifact;
        try {
            LocalProject localProject = null;
            if (appArtifact == null) {
                if (projectRoot == null) {
                    throw new IllegalArgumentException(
                            "Neither the application artifact nor the project root path has been provided");
                }
                localProject = enableClasspathCache ? loadWorkspace() : LocalProject.load(projectRoot, false);
                if (localProject == null) {
                    log.warn("Unable to locate the maven project on the filesystem");
                    throw new BootstrapException(
                            "Failed to determine the Maven artifact associated with the application " + projectRoot);
                }
                appArtifact = localProject.getAppArtifact();
            }

            Path cachedCpPath = null;

            LocalWorkspace workspace = null;
            if (enableClasspathCache) {
                if (localProject == null) {
                    localProject = loadWorkspace();
                }
                workspace = localProject.getWorkspace();
                cachedCpPath = resolveCachedCpPath(localProject);
                if (Files.exists(cachedCpPath)
                        && workspace.getLastModified() < Files.getLastModifiedTime(cachedCpPath).toMillis()) {
                    try {
                        final ApplicationModel appModel = readAppModelWithWorkspaceId(cachedCpPath, workspace.getId());
                        if (appModel != null) {
                            return new CurationResult(appModel);
                        }
                    } catch (IOException e) {
                        log.warn("Failed to read deployment classpath cache from " + cachedCpPath + " for "
                                + appArtifact, e);
                    }
                }
            }
            CurationResult curationResult = new CurationResult(getAppModelResolver()
                    .resolveManagedModel(appArtifact, forcedDependencies, managingProject, reloadableModules));
            if (cachedCpPath != null) {
                Files.createDirectories(cachedCpPath.getParent());
                try {
                    writeAppModelWithWorkspaceId(curationResult.getApplicationModel(), workspace.getId(), cachedCpPath);
                } catch (IOException e) {
                    log.warn("Failed to write classpath cache", e);
                }
            }
            return curationResult;
        } catch (Exception e) {
            throw new BootstrapException("Failed to create the application model for " + appArtifact, e);
        }
    }

    /**
     * Attempts to load an application model from a file system path set as a value of a system property.
     * In test mode the system property will be {@link BootstrapConstants#SERIALIZED_TEST_APP_MODEL}, otherwise
     * it will be {@link BootstrapConstants#SERIALIZED_APP_MODEL}.
     * <p>
     * If the property was not set, the method will return null.
     * <p>
     * If the model could not deserialized, an error will be logged and null returned.
     *
     * @return deserialized application model or null
     */
    private CurationResult loadFromSystemProperty() {
        // gradle tests and dev encode the result on the class path
        final String serializedModel = test ? System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL)
                : System.getProperty(BootstrapConstants.SERIALIZED_APP_MODEL);
        if (serializedModel != null) {
            final Path p = Paths.get(serializedModel);
            if (Files.exists(p)) {
                try (InputStream existing = Files.newInputStream(p)) {
                    return new CurationResult((ApplicationModel) new ObjectInputStream(existing).readObject());
                } catch (IOException | ClassNotFoundException e) {
                    log.error("Failed to load serialized app mode", e);
                }
                IoUtils.recursiveDelete(p);
            } else {
                log.error("Failed to locate serialized application model at " + serializedModel);
            }
        }
        return null;
    }

    private boolean isWorkspaceDiscoveryEnabled() {
        return localProjectsDiscovery == null ? projectRoot != null && (test || devMode)
                : localProjectsDiscovery;
    }

    private LocalProject loadWorkspace() throws AppModelResolverException {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) {
            return null;
        }
        LocalProject project = createBootstrapMavenContext().getCurrentProject();
        if (project == null) {
            return null;
        }
        if (project.getDir().equals(projectRoot)) {
            return project;
        }
        for (LocalProject p : project.getWorkspace().getProjects().values()) {
            if (p.getDir().equals(projectRoot)) {
                return p;
            }
        }
        log.warnf("Expected project directory %s does not match current project directory %s", projectRoot, project.getDir());
        return project;
    }

    /**
     * Checks whether the project path is a JAR and if it is, creates an application model for it.
     * If the project path is not a JAR, the method will return null.
     *
     * @param appArtifactPath application artifact path
     * @return resolved application model or null
     */
    private CurationResult createAppModelForJarOrNull(Path appArtifactPath) {
        if (projectRoot != null
                && (!Files.isDirectory(projectRoot) || projectRoot.getFileSystem().getClass().getName().contains("Zip"))) {
            AppModelResolver modelResolver = getAppModelResolver();
            final ApplicationModel appModel;
            ResolvedDependency appArtifact = this.appArtifact;
            try {
                if (appArtifact == null) {
                    appArtifact = ModelUtils.resolveAppArtifact(appArtifactPath);
                }
                modelResolver.relink(appArtifact, appArtifactPath);
                //we need some way to figure out dependencies here
                appModel = modelResolver.resolveManagedModel(appArtifact, List.of(), managingProject,
                        reloadableModules);
            } catch (AppModelResolverException | IOException e) {
                throw new RuntimeException("Failed to resolve initial application dependencies", e);
            }
            return new CurationResult(appModel);
        }
        return null;
    }

    private Path resolveCachedCpPath(LocalProject project) {
        if (test) {
            return BootstrapUtils.getSerializedTestAppModelPath(project.getOutputDir());
        }
        if (devMode) {
            return BootstrapUtils.resolveSerializedAppModelPath(project.getOutputDir());
        }
        return project.getOutputDir().resolve(QUARKUS).resolve(BOOTSTRAP).resolve(APP_MODEL_DAT);
    }

    public BootstrapAppModelFactory setMavenArtifactResolver(MavenArtifactResolver mavenArtifactResolver) {
        this.mavenArtifactResolver = mavenArtifactResolver;
        return this;
    }

    public BootstrapAppModelFactory setManagingProject(ArtifactCoords managingProject) {
        this.managingProject = managingProject;
        return this;
    }
}

package io.quarkus.bootstrap.app;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.classloading.ClassLoaderEventListener;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

/**
 * The entry point for starting/building a Quarkus application. This class sets up the base class loading
 * architecture. Once this has been established control is passed into the new class loaders
 * to allow for customisation of the boot process.
 */
public class QuarkusBootstrap implements Serializable {

    private static final long serialVersionUID = -3400622859354530408L;

    /**
     * The root of the application, where the application classes live.
     */
    private final PathCollection applicationRoot;

    /**
     * The root of the project. This may be different from the application root for tests that
     * run in a different directory.
     */
    private final Path projectRoot;

    /**
     * Any additional application archives that should be added to the application, that would not be otherwise
     * discovered. The main used case for this is testing to add src/test to the application even if it does
     * not have a beans.xml.
     */
    private final List<AdditionalDependency> additionalApplicationArchives;

    /**
     * Additional archives that are added to the augmentation class path
     */
    private final List<Path> additionalDeploymentArchives;

    /**
     * Any paths that should never be part of the application. This can be used to exclude the main src/test directory when
     * doing
     * unit testing, to make sure only the generated test archive is picked up.
     */
    private final List<Path> excludeFromClassPath;

    private final Properties buildSystemProperties;
    private final Properties runtimeProperties;
    private final String baseName;
    private final String originalBaseName;
    private final Path targetDirectory;

    private final Mode mode;
    private final Boolean offline;
    private final boolean test;
    private final Boolean localProjectDiscovery;

    private final ClassLoader baseClassLoader;
    private final AppModelResolver appModelResolver;

    private final ResolvedDependency appArtifact;
    private final boolean isolateDeployment;
    private final MavenArtifactResolver mavenArtifactResolver;
    private final ArtifactCoords managingProject;
    private final List<Dependency> forcedDependencies;
    private final boolean disableClasspathCache;
    private final ApplicationModel existingModel;
    private final boolean rebuild;
    private final Set<ArtifactKey> localArtifacts;
    private final List<ClassLoaderEventListener> classLoadListeners;
    private final boolean auxiliaryApplication;
    private final boolean hostApplicationIsTestOnly;
    private final boolean assertionsEnabled;
    private final boolean defaultFlatTestClassPath;
    private final Collection<ArtifactKey> parentFirstArtifacts;
    private final Supplier<DependencyInfoProvider> depInfoProvider;

    private QuarkusBootstrap(Builder builder) {
        this.applicationRoot = builder.applicationRoot;
        this.additionalApplicationArchives = new ArrayList<>(builder.additionalApplicationArchives);
        this.excludeFromClassPath = new ArrayList<>(builder.excludeFromClassPath);
        this.projectRoot = builder.projectRoot != null ? builder.projectRoot.normalize() : null;
        this.buildSystemProperties = builder.buildSystemProperties != null ? builder.buildSystemProperties : new Properties();
        this.runtimeProperties = builder.runtimeProperties != null ? builder.runtimeProperties : new Properties();
        this.mode = builder.mode;
        this.offline = builder.offline;
        this.test = builder.test;
        this.localProjectDiscovery = builder.localProjectDiscovery;
        this.baseName = builder.baseName;
        this.originalBaseName = builder.originalJarName;
        this.baseClassLoader = builder.baseClassLoader;
        this.targetDirectory = builder.targetDirectory;
        this.appModelResolver = builder.appModelResolver;
        this.assertionsEnabled = builder.assertionsEnabled;
        this.appArtifact = builder.appArtifact;
        this.isolateDeployment = builder.isolateDeployment;
        this.additionalDeploymentArchives = builder.additionalDeploymentArchives;
        this.mavenArtifactResolver = builder.mavenArtifactResolver;
        this.managingProject = builder.managingProject;
        this.forcedDependencies = new ArrayList<>(builder.forcedDependencies);
        this.disableClasspathCache = builder.disableClasspathCache;
        this.existingModel = builder.existingModel;
        this.rebuild = builder.rebuild;
        this.localArtifacts = new HashSet<>(builder.localArtifacts);
        this.classLoadListeners = builder.classLoadListeners;
        this.auxiliaryApplication = builder.auxiliaryApplication;
        this.hostApplicationIsTestOnly = builder.hostApplicationIsTestOnly;
        this.defaultFlatTestClassPath = builder.flatClassPath;
        this.parentFirstArtifacts = builder.parentFirstArtifacts;
        this.depInfoProvider = builder.depInfoProvider;
    }

    public CuratedApplication bootstrap() throws BootstrapException {
        //all we want to do is resolve all our dependencies
        //once we have this it is up to augment to set up the class loader to actually use them
        final CurationResult curationResult = existingModel != null
                ? new CurationResult(existingModel)
                : newAppModelFactory().resolveAppModel();

        if (curationResult.getApplicationModel().getAppArtifact() != null) {
            if (curationResult.getApplicationModel().getAppArtifact().getArtifactId() != null) {
                buildSystemProperties.putIfAbsent("quarkus.application.name",
                        curationResult.getApplicationModel().getAppArtifact().getArtifactId());
            }
            if (curationResult.getApplicationModel().getAppArtifact().getVersion() != null) {
                buildSystemProperties.putIfAbsent("quarkus.application.version",
                        curationResult.getApplicationModel().getAppArtifact().getVersion());
            }
        }

        final ConfiguredClassLoading classLoadingConfig = ConfiguredClassLoading.builder()
                .setApplicationRoot(applicationRoot)
                .setDefaultFlatTestClassPath(defaultFlatTestClassPath)
                .setMode(mode)
                .addParentFirstArtifacts(parentFirstArtifacts)
                .setApplicationModel(curationResult.getApplicationModel())
                .build();
        return new CuratedApplication(this, curationResult, classLoadingConfig);
    }

    public BootstrapAppModelFactory newAppModelFactory() {
        final BootstrapAppModelFactory appModelFactory = BootstrapAppModelFactory.newInstance()
                .setOffline(offline)
                .setMavenArtifactResolver(mavenArtifactResolver)
                .setBootstrapAppModelResolver(appModelResolver)
                .setLocalProjectsDiscovery(localProjectDiscovery)
                .setAppArtifact(appArtifact)
                .setManagingProject(managingProject)
                .setForcedDependencies(forcedDependencies)
                .setLocalArtifacts(localArtifacts)
                .setProjectRoot(projectRoot);
        if (mode == Mode.TEST || test) {
            appModelFactory.setTest(true);
            if (!disableClasspathCache) {
                appModelFactory.setEnableClasspathCache(true);
            }
        }
        if (mode == Mode.DEV) {
            appModelFactory.setDevMode(true);
            if (!disableClasspathCache) {
                appModelFactory.setEnableClasspathCache(true);
            }
        }
        return appModelFactory;
    }

    public PathCollection getApplicationRoot() {
        return applicationRoot;
    }

    public List<AdditionalDependency> getAdditionalApplicationArchives() {
        return Collections.unmodifiableList(additionalApplicationArchives);
    }

    public List<Path> getAdditionalDeploymentArchives() {
        return Collections.unmodifiableList(additionalDeploymentArchives);
    }

    public List<Path> getExcludeFromClassPath() {
        return Collections.unmodifiableList(excludeFromClassPath);
    }

    public Properties getBuildSystemProperties() {
        return buildSystemProperties;
    }

    public Properties getRuntimeProperties() {
        return runtimeProperties;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isAuxiliaryApplication() {
        return auxiliaryApplication;
    }

    public boolean isHostApplicationIsTestOnly() {
        return hostApplicationIsTestOnly;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Deprecated
    public static Builder builder(Path applicationRoot) {
        return new Builder().setApplicationRoot(PathList.of(applicationRoot));
    }

    public String getBaseName() {
        return baseName;
    }

    public String getOriginalBaseName() {
        return originalBaseName;
    }

    public ClassLoader getBaseClassLoader() {
        return baseClassLoader;
    }

    public Path getTargetDirectory() {
        return targetDirectory;
    }

    public boolean isIsolateDeployment() {
        return isolateDeployment;
    }

    public boolean isRebuild() {
        return rebuild;
    }

    public boolean isAssertionsEnabled() {
        return assertionsEnabled;
    }

    public List<ClassLoaderEventListener> getClassLoaderEventListeners() {
        return this.classLoadListeners;
    }

    public Builder clonedBuilder() {
        Builder builder = new Builder()
                .setBaseName(baseName)
                .setOriginalBaseName(originalBaseName)
                .setProjectRoot(projectRoot)
                .setBaseClassLoader(baseClassLoader)
                .setBuildSystemProperties(buildSystemProperties)
                .setRuntimeProperties(runtimeProperties)
                .setMode(mode)
                .setTest(test)
                .setLocalProjectDiscovery(localProjectDiscovery)
                .setTargetDirectory(targetDirectory)
                .setAppModelResolver(appModelResolver)
                .setAssertionsEnabled(assertionsEnabled)
                .setIsolateDeployment(isolateDeployment)
                .setMavenArtifactResolver(mavenArtifactResolver)
                .setManagingProject(managingProject)
                .setForcedDependencies(forcedDependencies)
                .setDisableClasspathCache(disableClasspathCache)
                .addClassLoaderEventListeners(classLoadListeners)
                .setExistingModel(existingModel);
        if (appArtifact != null) {
            builder.setAppArtifact(appArtifact);
        } else {
            builder.setApplicationRoot(applicationRoot);
        }
        if (offline != null) {
            builder.setOffline(offline);
        }
        builder.additionalApplicationArchives.addAll(additionalApplicationArchives);
        builder.additionalDeploymentArchives.addAll(additionalDeploymentArchives);
        builder.excludeFromClassPath.addAll(excludeFromClassPath);
        builder.localArtifacts.addAll(localArtifacts);
        return builder;
    }

    public boolean isTest() {
        return test;
    }

    public Supplier<DependencyInfoProvider> getDependencyInfoProvider() {
        return depInfoProvider;
    }

    public static class Builder {
        public List<ClassLoaderEventListener> classLoadListeners = new ArrayList<>();
        public boolean hostApplicationIsTestOnly;
        boolean flatClassPath;
        boolean rebuild;
        PathCollection applicationRoot;
        String baseName;
        String originalJarName;
        Path projectRoot;
        ClassLoader baseClassLoader = ClassLoader.getSystemClassLoader();
        final List<AdditionalDependency> additionalApplicationArchives = new ArrayList<>();
        final List<Path> additionalDeploymentArchives = new ArrayList<>();
        final List<Path> excludeFromClassPath = new ArrayList<>();
        Properties buildSystemProperties;
        Properties runtimeProperties;
        Mode mode = Mode.PROD;
        Boolean offline;
        boolean test;
        Boolean localProjectDiscovery;
        Path targetDirectory;
        AppModelResolver appModelResolver;
        boolean assertionsEnabled = inheritedAssertionsEnabled();
        ResolvedDependency appArtifact;
        boolean isolateDeployment;
        MavenArtifactResolver mavenArtifactResolver;
        ArtifactCoords managingProject;
        List<Dependency> forcedDependencies = Collections.emptyList();
        boolean disableClasspathCache;
        ApplicationModel existingModel;
        final Set<ArtifactKey> localArtifacts = new HashSet<>();
        boolean auxiliaryApplication;
        List<ArtifactKey> parentFirstArtifacts = new ArrayList<>();
        Supplier<DependencyInfoProvider> depInfoProvider;

        public Builder() {
        }

        public Builder setApplicationRoot(Path applicationRoot) {
            this.applicationRoot = PathList.of(applicationRoot);
            return this;
        }

        public Builder setApplicationRoot(PathCollection applicationRoot) {
            if (appArtifact != null) {
                throw new IllegalStateException("Cannot set both app artifact and application root");
            }
            this.applicationRoot = applicationRoot;
            return this;
        }

        public Builder addAdditionalApplicationArchive(AdditionalDependency path) {
            additionalApplicationArchives.add(path);
            return this;
        }

        public Builder addAdditionalApplicationArchives(Collection<AdditionalDependency> path) {
            additionalApplicationArchives.addAll(path);
            return this;
        }

        public Builder addAdditionalDeploymentArchive(Path path) {
            additionalDeploymentArchives.add(path);
            return this;
        }

        public Builder setFlatClassPath(boolean flatClassPath) {
            this.flatClassPath = flatClassPath;
            return this;
        }

        public Builder addExcludedPath(Path path) {
            excludeFromClassPath.add(path);
            return this;
        }

        /**
         * The project root, used only for project dependency discovery.
         */
        public Builder setProjectRoot(Path projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }

        public Builder setBuildSystemProperties(Properties buildSystemProperties) {
            this.buildSystemProperties = buildSystemProperties;
            return this;
        }

        public Builder setRuntimeProperties(Properties runtimeProperties) {
            this.runtimeProperties = runtimeProperties;
            return this;
        }

        public Builder setOffline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public Builder setTest(boolean test) {
            this.test = test;
            return this;
        }

        public Builder setMode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder setHostApplicationIsTestOnly(boolean hostApplicationIsTestOnly) {
            this.hostApplicationIsTestOnly = hostApplicationIsTestOnly;
            return this;
        }

        public Builder setAuxiliaryApplication(boolean auxiliaryApplication) {
            this.auxiliaryApplication = auxiliaryApplication;
            return this;
        }

        public Builder setLocalProjectDiscovery(Boolean localProjectDiscovery) {
            this.localProjectDiscovery = localProjectDiscovery;
            return this;
        }

        public Builder setBaseName(String baseName) {
            this.baseName = baseName;
            return this;
        }

        public Builder setOriginalBaseName(String originalJarName) {
            this.originalJarName = originalJarName;
            return this;
        }

        public Builder setBaseClassLoader(ClassLoader baseClassLoader) {
            this.baseClassLoader = baseClassLoader;
            return this;
        }

        public Builder setTargetDirectory(Path targetDirectory) {
            this.targetDirectory = targetDirectory;
            return this;
        }

        public Builder setAppModelResolver(AppModelResolver appModelResolver) {
            this.appModelResolver = appModelResolver;
            return this;
        }

        public Builder setDisableClasspathCache(boolean disableClasspathCache) {
            this.disableClasspathCache = disableClasspathCache;
            return this;
        }

        public Builder addParentFirstArtifact(ArtifactKey appArtifactKey) {
            this.parentFirstArtifacts.add(appArtifactKey);
            return this;
        }

        /**
         * The app artifact. Note that if you want to use this as the basis of the application
         * you must also explicitly set the application root to this artifacts paths.
         */
        public Builder setAppArtifact(ResolvedDependency appArtifact) {
            if (applicationRoot != null) {
                throw new IllegalStateException("Cannot set both application root and app artifact");
            }
            this.appArtifact = appArtifact;
            this.applicationRoot = PathList.from(appArtifact.getResolvedPaths());
            if (appArtifact.getResolvedPaths().isSinglePath()) {
                this.projectRoot = appArtifact.getResolvedPaths().getSinglePath();
            }
            return this;
        }

        public Builder setManagingProject(ArtifactCoords managingProject) {
            this.managingProject = managingProject;
            return this;
        }

        /**
         * If the deployment should use an isolated (aka parent last) classloader.
         * <p>
         * For tests this is generally false, as we want to share the base class path so that the
         * test extension code can integrate with the deployment.
         * <p>
         * TODO: should this always be true?
         *
         * @param isolateDeployment
         * @return
         */
        public Builder setIsolateDeployment(boolean isolateDeployment) {
            this.isolateDeployment = isolateDeployment;
            return this;
        }

        public Builder setMavenArtifactResolver(MavenArtifactResolver mavenArtifactResolver) {
            this.mavenArtifactResolver = mavenArtifactResolver;
            return this;
        }

        /**
         * If set, each of these dependencies will either be added to the application dependencies if the GA doesn't match any
         * application dependencies, or override the existing version if the GA does match
         */
        public Builder setForcedDependencies(List<Dependency> forcedDependencies) {
            this.forcedDependencies = forcedDependencies;
            return this;
        }

        public ApplicationModel getExistingModel() {
            return existingModel;
        }

        public Builder setExistingModel(ApplicationModel existingModel) {
            this.existingModel = existingModel;
            return this;
        }

        public Builder addLocalArtifact(ArtifactKey key) {
            localArtifacts.add(key);
            return this;
        }

        public Builder clearLocalArtifacts() {
            localArtifacts.clear();
            return this;
        }

        public Builder setRebuild(boolean value) {
            this.rebuild = value;
            return this;
        }

        public Builder addClassLoaderEventListeners(List<ClassLoaderEventListener> classLoadListeners) {
            this.classLoadListeners.addAll(classLoadListeners);
            return this;
        }

        public Builder setAssertionsEnabled(boolean assertionsEnabled) {
            this.assertionsEnabled = assertionsEnabled;
            return this;
        }

        private boolean inheritedAssertionsEnabled() {
            boolean result = false;
            assert result = true;
            return result;
        }

        public Builder setDependencyInfoProvider(Supplier<DependencyInfoProvider> depInfoProvider) {
            this.depInfoProvider = depInfoProvider;
            return this;
        }

        public QuarkusBootstrap build() {
            Objects.requireNonNull(applicationRoot, "Application root must not be null");
            if (appArtifact != null) {
                localArtifacts.add(appArtifact.getKey());
            }
            return new QuarkusBootstrap(this);
        }
    }

    public enum Mode {
        DEV,
        TEST,
        PROD,
        REMOTE_DEV_SERVER,
        REMOTE_DEV_CLIENT,
        CONTINUOUS_TEST;
    }
}

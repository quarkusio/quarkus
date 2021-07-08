package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.classloading.ClassLoaderEventListener;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.update.DependenciesOrigin;
import io.quarkus.bootstrap.resolver.update.VersionUpdate;
import io.quarkus.bootstrap.resolver.update.VersionUpdateNumber;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * The entry point for starting/building a Quarkus application. This class sets up the base class loading
 * architecture. Once this has been established control is passed into the new class loaders
 * to allow for customisation of the boot process.
 */
public class QuarkusBootstrap implements Serializable {

    /**
     * The root of the application, where the application classes live.
     */
    private final PathsCollection applicationRoot;

    /**
     * The root of the project. This may be different to the application root for tests that
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
    private final String baseName;
    private final Path targetDirectory;

    private final Mode mode;
    private final Boolean offline;
    private final boolean test;
    private final Boolean localProjectDiscovery;

    private final ClassLoader baseClassLoader;
    private final AppModelResolver appModelResolver;

    private final VersionUpdateNumber versionUpdateNumber;
    private final VersionUpdate versionUpdate;
    private final DependenciesOrigin dependenciesOrigin;
    private final AppArtifact appArtifact;
    private final boolean isolateDeployment;
    private final MavenArtifactResolver mavenArtifactResolver;
    private final AppArtifact managingProject;
    private final List<AppDependency> forcedDependencies;
    private final boolean disableClasspathCache;
    private final AppModel existingModel;
    private final boolean rebuild;
    private final Set<AppArtifactKey> localArtifacts;
    private final List<ClassLoaderEventListener> classLoadListeners;
    private final boolean auxiliaryApplication;
    private final boolean hostApplicationIsTestOnly;
    private final boolean flatClassPath;
    private final ConfiguredClassLoading classLoadingConfig;
    private final boolean assertionsEnabled;

    private QuarkusBootstrap(Builder builder, ConfiguredClassLoading classLoadingConfig) {
        this.applicationRoot = builder.applicationRoot;
        this.additionalApplicationArchives = new ArrayList<>(builder.additionalApplicationArchives);
        this.excludeFromClassPath = new ArrayList<>(builder.excludeFromClassPath);
        this.projectRoot = builder.projectRoot != null ? builder.projectRoot.normalize() : null;
        this.buildSystemProperties = builder.buildSystemProperties;
        this.mode = builder.mode;
        this.offline = builder.offline;
        this.test = builder.test;
        this.localProjectDiscovery = builder.localProjectDiscovery;
        this.baseName = builder.baseName;
        this.baseClassLoader = builder.baseClassLoader;
        this.targetDirectory = builder.targetDirectory;
        this.appModelResolver = builder.appModelResolver;
        this.assertionsEnabled = builder.assertionsEnabled;
        this.versionUpdate = builder.versionUpdate;
        this.versionUpdateNumber = builder.versionUpdateNumber;
        this.dependenciesOrigin = builder.dependenciesOrigin;
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
        this.flatClassPath = builder.flatClassPath;
        this.classLoadingConfig = classLoadingConfig;
        this.hostApplicationIsTestOnly = builder.hostApplicationIsTestOnly;
    }

    public CuratedApplication bootstrap() throws BootstrapException {
        //all we want to do is resolve all our dependencies
        //once we have this it is up to augment to set up the class loader to actually use them

        if (existingModel != null) {
            return new CuratedApplication(this, new CurationResult(existingModel), classLoadingConfig);
        }
        //first we check for updates
        if (mode != Mode.PROD) {
            if (versionUpdate != VersionUpdate.NONE) {
                throw new BootstrapException(
                        "updates are only supported for PROD mode for existing files, not for dev or test");
            }
        }

        BootstrapAppModelFactory appModelFactory = BootstrapAppModelFactory.newInstance()
                .setOffline(offline)
                .setMavenArtifactResolver(mavenArtifactResolver)
                .setBootstrapAppModelResolver(appModelResolver)
                .setVersionUpdate(versionUpdate)
                .setVersionUpdateNumber(versionUpdateNumber)
                .setDependenciesOrigin(dependenciesOrigin)
                .setLocalProjectsDiscovery(localProjectDiscovery)
                .setAppArtifact(appArtifact)
                .setManagingProject(managingProject)
                .setForcedDependencies(forcedDependencies)
                .setLocalArtifacts(localArtifacts)
                .setProjectRoot(getProjectRoot());
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
        return new CuratedApplication(this, appModelFactory.resolveAppModel(), classLoadingConfig);
    }

    private static ConfiguredClassLoading createClassLoadingConfig(PathsCollection applicationRoot, Mode mode) {
        //look for an application.properties
        for (Path path : applicationRoot) {
            Path props = path.resolve("application.properties");
            if (Files.exists(props)) {
                try (InputStream in = Files.newInputStream(props)) {
                    Properties p = new Properties();
                    p.load(in);
                    Set<AppArtifactKey> parentFirst = toArtifactSet(
                            p.getProperty(selectKey("quarkus.class-loading.parent-first-artifacts", p, mode)));
                    Set<AppArtifactKey> liveReloadable = toArtifactSet(
                            p.getProperty(selectKey("quarkus.class-loading.reloadable-artifacts", p, mode)));
                    boolean flatClassPath = Boolean.parseBoolean(
                            p.getProperty(selectKey("quarkus.test.flat-class-path", p, mode)));
                    return new ConfiguredClassLoading(parentFirst, liveReloadable, flatClassPath);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load bootstrap classloading config from application.properties", e);
                }
            }
        }
        return new ConfiguredClassLoading(Collections.emptySet(), Collections.emptySet(), false);
    }

    private static String selectKey(String base, Properties p, Mode mode) {
        String profile = BootstrapProfile.getActiveProfile(mode);
        String profileKey = "%" + profile + "." + base;
        if (p.containsKey(profileKey)) {
            return profileKey;
        }
        return base;
    }

    private static Set<AppArtifactKey> toArtifactSet(String config) {
        if (config == null) {
            return new HashSet<>();
        }
        Set<AppArtifactKey> ret = new HashSet<>();
        for (String i : config.split(",")) {
            ret.add(new AppArtifactKey(i.split(":")));
        }
        return ret;
    }

    public AppModelResolver getAppModelResolver() {
        return appModelResolver;
    }

    public PathsCollection getApplicationRoot() {
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
        return new Builder().setApplicationRoot(PathsCollection.of(applicationRoot));
    }

    public String getBaseName() {
        return baseName;
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
                .setProjectRoot(projectRoot)
                .setBaseClassLoader(baseClassLoader)
                .setBuildSystemProperties(buildSystemProperties)
                .setMode(mode)
                .setTest(test)
                .setLocalProjectDiscovery(localProjectDiscovery)
                .setTargetDirectory(targetDirectory)
                .setAppModelResolver(appModelResolver)
                .setAssertionsEnabled(assertionsEnabled)
                .setVersionUpdateNumber(versionUpdateNumber)
                .setVersionUpdate(versionUpdate)
                .setDependenciesOrigin(dependenciesOrigin)
                .setIsolateDeployment(isolateDeployment)
                .setMavenArtifactResolver(mavenArtifactResolver)
                .setManagingProject(managingProject)
                .setForcedDependencies(new ArrayList<>(forcedDependencies))
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

    public boolean isFlatClassPath() {
        return flatClassPath;
    }

    public static class Builder {
        public List<ClassLoaderEventListener> classLoadListeners = new ArrayList<>();
        public boolean hostApplicationIsTestOnly;
        boolean flatClassPath;
        boolean rebuild;
        PathsCollection applicationRoot;
        String baseName;
        Path projectRoot;
        ClassLoader baseClassLoader = ClassLoader.getSystemClassLoader();
        final List<AdditionalDependency> additionalApplicationArchives = new ArrayList<>();
        final List<Path> additionalDeploymentArchives = new ArrayList<>();
        final List<Path> excludeFromClassPath = new ArrayList<>();
        Properties buildSystemProperties;
        Mode mode = Mode.PROD;
        Boolean offline;
        boolean test;
        Boolean localProjectDiscovery;
        Path targetDirectory;
        AppModelResolver appModelResolver;
        boolean assertionsEnabled = inheritedAssertionsEnabled();
        VersionUpdateNumber versionUpdateNumber = VersionUpdateNumber.MICRO;
        VersionUpdate versionUpdate = VersionUpdate.NONE;
        DependenciesOrigin dependenciesOrigin;
        AppArtifact appArtifact;
        boolean isolateDeployment;
        MavenArtifactResolver mavenArtifactResolver;
        AppArtifact managingProject;
        List<AppDependency> forcedDependencies = new ArrayList<>();
        boolean disableClasspathCache;
        AppModel existingModel;
        final Set<AppArtifactKey> localArtifacts = new HashSet<>();
        boolean auxiliaryApplication;

        public Builder() {
        }

        public Builder setApplicationRoot(Path applicationRoot) {
            this.applicationRoot = PathsCollection.of(applicationRoot);
            return this;
        }

        public Builder setApplicationRoot(PathsCollection applicationRoot) {
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

        public Builder setVersionUpdateNumber(VersionUpdateNumber versionUpdateNumber) {
            this.versionUpdateNumber = versionUpdateNumber;
            return this;
        }

        public Builder setVersionUpdate(VersionUpdate versionUpdate) {
            this.versionUpdate = versionUpdate;
            return this;
        }

        public Builder setDependenciesOrigin(DependenciesOrigin dependenciesOrigin) {
            this.dependenciesOrigin = dependenciesOrigin;
            return this;
        }

        public Builder setDisableClasspathCache(boolean disableClasspathCache) {
            this.disableClasspathCache = disableClasspathCache;
            return this;
        }

        /**
         * The app artifact. Note that if you want to use this as the basis of the application
         * you must also explicitly set the application root to this artifacts paths.
         */
        public Builder setAppArtifact(AppArtifact appArtifact) {
            if (applicationRoot != null) {
                throw new IllegalStateException("Cannot set both application root and app artifact");
            }
            this.appArtifact = appArtifact;
            this.applicationRoot = appArtifact.getPaths();
            if (appArtifact.getPaths().isSinglePath()) {
                this.projectRoot = appArtifact.getPaths().getSinglePath();
            }
            return this;
        }

        public Builder setManagingProject(AppArtifact managingProject) {
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
        public Builder setForcedDependencies(List<AppDependency> forcedDependencies) {
            this.forcedDependencies = forcedDependencies;
            return this;
        }

        public AppModel getExistingModel() {
            return existingModel;
        }

        public Builder setExistingModel(AppModel existingModel) {
            this.existingModel = existingModel;
            return this;
        }

        public Builder addLocalArtifact(AppArtifactKey key) {
            localArtifacts.add(key);
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

        @SuppressWarnings("AssertWithSideEffects")
        private boolean inheritedAssertionsEnabled() {
            boolean result = false;
            assert result = true;
            return result;
        }

        public QuarkusBootstrap build() {
            Objects.requireNonNull(applicationRoot, "Application root must not be null");
            if (appArtifact != null) {
                localArtifacts.add(appArtifact.getKey());
            }

            ConfiguredClassLoading classLoadingConfig = createClassLoadingConfig(applicationRoot, mode);
            if (classLoadingConfig.flatTestClassPath && mode == Mode.TEST) {
                flatClassPath = true;
            }
            return new QuarkusBootstrap(this, classLoadingConfig);
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

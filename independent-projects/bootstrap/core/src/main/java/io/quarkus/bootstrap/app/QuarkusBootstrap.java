package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.update.DependenciesOrigin;
import io.quarkus.bootstrap.resolver.update.VersionUpdate;
import io.quarkus.bootstrap.resolver.update.VersionUpdateNumber;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * The entry point for starting/building a Quarkus application. This class sets up the base class loading
 * architecture. Once this has been established control is passed into the new class loaders
 * to allow for customisation of the boot process.
 *
 */
public class QuarkusBootstrap implements Serializable {

    /**
     * The root of the application, where the application classes live.
     */
    private final Path applicationRoot;

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
    private final boolean offline;
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

    private QuarkusBootstrap(Builder builder) {
        this.applicationRoot = builder.applicationRoot;
        this.additionalApplicationArchives = new ArrayList<>(builder.additionalApplicationArchives);
        if (applicationRoot != null) {
            // this path has to be added last to give a chance the test directories override the app properties, etc
            additionalApplicationArchives.add(AdditionalDependency.test(applicationRoot));
        }
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
        this.versionUpdate = builder.versionUpdate;
        this.versionUpdateNumber = builder.versionUpdateNumber;
        this.dependenciesOrigin = builder.dependenciesOrigin;
        this.appArtifact = builder.appArtifact;
        this.isolateDeployment = builder.isolateDeployment;
        this.additionalDeploymentArchives = builder.additionalDeploymentArchives;
        this.mavenArtifactResolver = builder.mavenArtifactResolver;
        this.managingProject = builder.managingProject;
        this.forcedDependencies = new ArrayList<>(builder.forcedDependencies);
    }

    public CuratedApplication bootstrap() throws BootstrapException {
        //all we want to do is resolve all our dependencies
        //once we have this it is up to augment to set up the class loader to actually use them

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
                .setProjectRoot(getProjectRoot() != null ? getProjectRoot()
                        : getApplicationRoot());
        if (mode == Mode.TEST || test) {
            appModelFactory.setTest(true);
            appModelFactory.setEnableClasspathCache(true);
        }
        if (mode == Mode.DEV) {
            appModelFactory.setDevMode(true);
            appModelFactory.setEnableClasspathCache(true);
        }
        return new CuratedApplication(this, appModelFactory.resolveAppModel());
    }

    public AppModelResolver getAppModelResolver() {
        return appModelResolver;
    }

    public Path getApplicationRoot() {
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

    public boolean isOffline() {
        return offline;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Deprecated
    public static Builder builder(Path applicationRoot) {
        return new Builder(applicationRoot);
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

    public static class Builder {
        Path applicationRoot;
        String baseName;
        Path projectRoot;
        ClassLoader baseClassLoader = ClassLoader.getSystemClassLoader();
        final List<AdditionalDependency> additionalApplicationArchives = new ArrayList<>();
        final List<Path> additionalDeploymentArchives = new ArrayList<>();
        final List<Path> excludeFromClassPath = new ArrayList<>();
        Properties buildSystemProperties;
        Mode mode = Mode.PROD;
        boolean offline;
        boolean test;
        Boolean localProjectDiscovery;
        Path targetDirectory;
        AppModelResolver appModelResolver;
        VersionUpdateNumber versionUpdateNumber = VersionUpdateNumber.MICRO;
        VersionUpdate versionUpdate = VersionUpdate.NONE;
        DependenciesOrigin dependenciesOrigin;
        AppArtifact appArtifact;
        boolean isolateDeployment;
        MavenArtifactResolver mavenArtifactResolver;
        AppArtifact managingProject;
        List<AppDependency> forcedDependencies = new ArrayList<>();

        public Builder() {
        }

        @Deprecated
        public Builder(Path applicationRoot) {
            setApplicationRoot(applicationRoot);
        }

        public Builder setApplicationRoot(Path applicationRoot) {
            this.applicationRoot = applicationRoot;
            return this;
        }

        public Builder addAdditionalApplicationArchive(AdditionalDependency path) {
            additionalApplicationArchives.add(path);
            return this;
        }

        public Builder addAdditionalDeploymentArchive(Path path) {
            additionalDeploymentArchives.add(path);
            return this;
        }

        public Builder addExcludedPath(Path path) {
            excludeFromClassPath.add(path);
            return this;
        }

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

        public Builder setLocalProjectDiscovery(boolean localProjectDiscovery) {
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

        public Builder setAppArtifact(AppArtifact appArtifact) {
            this.appArtifact = appArtifact;
            return this;
        }

        public Builder setManagingProject(AppArtifact managingProject) {
            this.managingProject = managingProject;
            return this;
        }

        /**
         * If the deployment should use an isolated (aka parent last) classloader.
         *
         * For tests this is generally false, as we want to share the base class path so that the
         * test extension code can integrate with the deployment.
         *
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

        public QuarkusBootstrap build() {
            return new QuarkusBootstrap(this);
        }
    }

    public enum Mode {
        DEV,
        TEST,
        PROD;
    }
}

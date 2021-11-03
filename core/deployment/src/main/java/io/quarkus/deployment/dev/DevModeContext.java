package io.quarkus.deployment.dev;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Object that is used to pass context data from the plugin doing the invocation
 * into the dev mode process using java serialization.
 *
 * There is no need to worry about compat as both sides will always be using the same version
 */
public class DevModeContext implements Serializable {

    public static final CompilationUnit EMPTY_COMPILATION_UNIT = new CompilationUnit(PathsCollection.of(), null, null, null);

    public static final String ENABLE_PREVIEW_FLAG = "--enable-preview";

    private ModuleInfo applicationRoot;
    private final List<ModuleInfo> additionalModules = new ArrayList<>();
    private final Map<String, String> systemProperties = new HashMap<>();
    private final Map<String, String> buildSystemProperties = new HashMap<>();
    private String sourceEncoding;

    private final List<URL> additionalClassPathElements = new ArrayList<>();
    private File cacheDir;
    private File projectDir;
    private boolean test;
    private boolean abortOnFailedStart;
    // the jar file which is used to launch the DevModeMain
    private File devModeRunnerJarFile;
    private boolean localProjectDiscovery = true;
    // args of the main-method
    private String[] args;

    private List<String> compilerOptions;
    private String sourceJavaVersion;
    private String targetJvmVersion;

    private List<String> compilerPluginArtifacts;
    private List<String> compilerPluginsOptions;

    private String alternateEntryPoint;
    private QuarkusBootstrap.Mode mode = QuarkusBootstrap.Mode.DEV;
    private String baseName;
    private final Set<AppArtifactKey> localArtifacts = new HashSet<>();

    public boolean isLocalProjectDiscovery() {
        return localProjectDiscovery;
    }

    public DevModeContext setLocalProjectDiscovery(boolean localProjectDiscovery) {
        this.localProjectDiscovery = localProjectDiscovery;
        return this;
    }

    public String getAlternateEntryPoint() {
        return alternateEntryPoint;
    }

    public DevModeContext setAlternateEntryPoint(String alternateEntryPoint) {
        this.alternateEntryPoint = alternateEntryPoint;
        return this;
    }

    public ModuleInfo getApplicationRoot() {
        return applicationRoot;
    }

    public DevModeContext setApplicationRoot(ModuleInfo applicationRoot) {
        this.applicationRoot = applicationRoot;
        return this;
    }

    public List<ModuleInfo> getAdditionalModules() {
        return additionalModules;
    }

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    public Map<String, String> getBuildSystemProperties() {
        return buildSystemProperties;
    }

    public String getSourceEncoding() {
        return sourceEncoding;
    }

    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    public List<URL> getAdditionalClassPathElements() {
        return additionalClassPathElements;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isAbortOnFailedStart() {
        return abortOnFailedStart;
    }

    public void setAbortOnFailedStart(boolean abortOnFailedStart) {
        this.abortOnFailedStart = abortOnFailedStart;
    }

    public List<String> getCompilerOptions() {
        return compilerOptions;
    }

    public void setCompilerOptions(List<String> compilerOptions) {
        this.compilerOptions = compilerOptions;
    }

    public String getSourceJavaVersion() {
        return sourceJavaVersion;
    }

    public void setSourceJavaVersion(String sourceJavaVersion) {
        this.sourceJavaVersion = sourceJavaVersion;
    }

    public String getTargetJvmVersion() {
        return targetJvmVersion;
    }

    public void setTargetJvmVersion(String targetJvmVersion) {
        this.targetJvmVersion = targetJvmVersion;
    }

    public List<String> getCompilerPluginArtifacts() {
        return compilerPluginArtifacts;
    }

    public void setCompilerPluginArtifacts(List<String> compilerPluginArtifacts) {
        this.compilerPluginArtifacts = compilerPluginArtifacts;
    }

    public List<String> getCompilerPluginsOptions() {
        return compilerPluginsOptions;
    }

    public void setCompilerPluginsOptions(List<String> compilerPluginsOptions) {
        this.compilerPluginsOptions = compilerPluginsOptions;
    }

    public File getDevModeRunnerJarFile() {
        return devModeRunnerJarFile;
    }

    public void setDevModeRunnerJarFile(final File devModeRunnerJarFile) {
        this.devModeRunnerJarFile = devModeRunnerJarFile;
    }

    public File getProjectDir() {
        return projectDir;
    }

    public DevModeContext setProjectDir(File projectDir) {
        this.projectDir = projectDir;
        return this;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public List<ModuleInfo> getAllModules() {
        List<ModuleInfo> ret = new ArrayList<>();
        ret.add(applicationRoot);
        ret.addAll(additionalModules);
        return ret;
    }

    public QuarkusBootstrap.Mode getMode() {
        return mode;
    }

    public void setMode(QuarkusBootstrap.Mode mode) {
        this.mode = mode;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public Set<AppArtifactKey> getLocalArtifacts() {
        return localArtifacts;
    }

    public static class ModuleInfo implements Serializable {

        private final ArtifactKey appArtifactKey;
        private final String name;
        private final String projectDirectory;
        private final CompilationUnit main;
        private final CompilationUnit test;

        private final String preBuildOutputDir;
        private final PathsCollection sourceParents;
        private final String targetDir;

        ModuleInfo(Builder builder) {
            this.appArtifactKey = builder.appArtifactKey;
            this.name = builder.name;
            this.projectDirectory = builder.projectDirectory;
            this.main = new CompilationUnit(builder.sourcePaths, builder.classesPath,
                    builder.resourcePaths,
                    builder.resourcesOutputPath);
            if (builder.testClassesPath != null) {
                this.test = new CompilationUnit(builder.testSourcePaths,
                        builder.testClassesPath, builder.testResourcePaths, builder.testResourcesOutputPath);
            } else {
                this.test = null;
            }
            this.sourceParents = builder.sourceParents;
            this.preBuildOutputDir = builder.preBuildOutputDir;
            this.targetDir = builder.targetDir;
        }

        public String getName() {
            return name;
        }

        public String getProjectDirectory() {
            return projectDirectory;
        }

        public PathsCollection getSourceParents() {
            return sourceParents;
        }

        //TODO: why isn't this immutable?
        public void addSourcePaths(Collection<String> additionalPaths) {
            this.main.sourcePaths = this.main.sourcePaths.add(
                    additionalPaths.stream()
                            .map(p -> Paths.get(p).isAbsolute() ? p : (projectDirectory + File.separator + p))
                            .map(Paths::get)
                            .toArray(Path[]::new));
        }

        public String getPreBuildOutputDir() {
            return preBuildOutputDir;
        }

        public String getTargetDir() {
            return targetDir;
        }

        public ArtifactKey getArtifactKey() {
            return appArtifactKey;
        }

        public CompilationUnit getMain() {
            return main;
        }

        public Optional<CompilationUnit> getTest() {
            return Optional.ofNullable(test);
        }

        public static class Builder {

            private ArtifactKey appArtifactKey;
            private String name;
            private String projectDirectory;
            private PathsCollection sourcePaths = PathsCollection.of();
            private String classesPath;
            private PathsCollection resourcePaths = PathsCollection.of();
            private String resourcesOutputPath;

            private String preBuildOutputDir;
            private PathsCollection sourceParents = PathsCollection.of();
            private String targetDir;

            private PathsCollection testSourcePaths = PathsCollection.of();
            private String testClassesPath;
            private PathsCollection testResourcePaths = PathsCollection.of();
            private String testResourcesOutputPath;

            public Builder setArtifactKey(ArtifactKey appArtifactKey) {
                this.appArtifactKey = appArtifactKey;
                return this;
            }

            public Builder setName(String name) {
                this.name = name;
                return this;
            }

            public Builder setProjectDirectory(String projectDirectory) {
                this.projectDirectory = projectDirectory;
                return this;
            }

            public Builder setSourcePaths(PathsCollection sourcePaths) {
                this.sourcePaths = sourcePaths;
                return this;
            }

            public Builder setClassesPath(String classesPath) {
                this.classesPath = classesPath;
                return this;
            }

            public Builder setResourcePaths(PathsCollection resourcePaths) {
                this.resourcePaths = resourcePaths;
                return this;
            }

            public Builder setResourcesOutputPath(String resourcesOutputPath) {
                this.resourcesOutputPath = resourcesOutputPath;
                return this;
            }

            public Builder setPreBuildOutputDir(String preBuildOutputDir) {
                this.preBuildOutputDir = preBuildOutputDir;
                return this;
            }

            public Builder setSourceParents(PathsCollection sourceParents) {
                this.sourceParents = sourceParents;
                return this;
            }

            public Builder setTargetDir(String targetDir) {
                this.targetDir = targetDir;
                return this;
            }

            public Builder setTestSourcePaths(PathsCollection testSourcePaths) {
                this.testSourcePaths = testSourcePaths;
                return this;
            }

            public Builder setTestClassesPath(String testClassesPath) {
                this.testClassesPath = testClassesPath;
                return this;
            }

            public Builder setTestResourcePaths(PathsCollection testResourcePaths) {
                this.testResourcePaths = testResourcePaths;
                return this;
            }

            public Builder setTestResourcesOutputPath(String testResourcesOutputPath) {
                this.testResourcesOutputPath = testResourcesOutputPath;
                return this;
            }

            public ModuleInfo build() {
                return new ModuleInfo(this);
            }
        }
    }

    public static class CompilationUnit implements Serializable {
        private PathsCollection sourcePaths;
        private final String classesPath;
        private final PathsCollection resourcePaths;
        private final String resourcesOutputPath;

        public CompilationUnit(PathsCollection sourcePaths, String classesPath, PathsCollection resourcePaths,
                String resourcesOutputPath) {
            this.sourcePaths = sourcePaths;
            this.classesPath = classesPath;
            this.resourcePaths = resourcePaths;
            this.resourcesOutputPath = resourcesOutputPath;
        }

        public PathsCollection getSourcePaths() {
            return sourcePaths;
        }

        public String getClassesPath() {
            return classesPath;
        }

        public PathsCollection getResourcePaths() {
            return resourcePaths;
        }

        public String getResourcesOutputPath() {
            return resourcesOutputPath;
        }
    }

    public boolean isEnablePreview() {
        if (compilerOptions == null) {
            return false;
        }
        return compilerOptions.contains(ENABLE_PREVIEW_FLAG);
    }
}

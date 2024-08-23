package io.quarkus.deployment.dev;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

/**
 * Object that is used to pass context data from the plugin doing the invocation
 * into the dev mode process using java serialization.
 *
 * There is no need to worry about compat as both sides will always be using the same version
 */
public class DevModeContext implements Serializable {

    private static final long serialVersionUID = 4688502145533897982L;

    public static final CompilationUnit EMPTY_COMPILATION_UNIT = new CompilationUnit(PathList.of(), null, null, null, null);

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

    private Map<String, Set<String>> compilerOptions;
    private String releaseJavaVersion;
    private String sourceJavaVersion;
    private String targetJvmVersion;

    private List<String> compilerPluginArtifacts;
    private List<String> compilerPluginsOptions;

    private String alternateEntryPoint;
    private QuarkusBootstrap.Mode mode = QuarkusBootstrap.Mode.DEV;
    private String baseName;
    private final Set<ArtifactKey> localArtifacts = new HashSet<>();

    private Set<File> processorPaths;
    private List<String> processors;

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

    public Map<String, Set<String>> getCompilerOptions() {
        return compilerOptions;
    }

    public void setCompilerOptions(Map<String, Set<String>> compilerOptions) {
        this.compilerOptions = compilerOptions;
    }

    public String getReleaseJavaVersion() {
        return releaseJavaVersion;
    }

    public void setReleaseJavaVersion(String releaseJavaVersion) {
        this.releaseJavaVersion = releaseJavaVersion;
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
        ret.addAll(additionalModules);
        ret.add(applicationRoot);
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

    public Set<ArtifactKey> getLocalArtifacts() {
        return localArtifacts;
    }

    public void setAnnotationProcessorPaths(Set<File> processorPaths) {
        this.processorPaths = processorPaths;
    }

    public Set<File> getAnnotationProcessorPaths() {
        return processorPaths;
    }

    public void setAnnotationProcessors(List<String> processors) {
        this.processors = processors;
    }

    public List<String> getAnnotationProcessors() {
        return processors;
    }

    public static class ModuleInfo implements Serializable {

        private static final long serialVersionUID = -1376678003747618410L;

        private final ArtifactKey appArtifactKey;
        private final String name;
        private final String projectDirectory;
        private final CompilationUnit main;
        private final CompilationUnit test;

        private final String preBuildOutputDir;
        private final PathCollection sourceParents;
        private final String targetDir;

        ModuleInfo(Builder builder) {
            this.appArtifactKey = builder.appArtifactKey;
            this.name = builder.name == null ? builder.appArtifactKey.toGacString() : builder.name;
            this.projectDirectory = builder.projectDirectory;
            this.main = new CompilationUnit(builder.sourcePaths, builder.classesPath,
                    builder.resourcePaths,
                    builder.resourcesOutputPath,
                    builder.generatedSourcesPath);

            if (builder.testClassesPath != null) {
                // FIXME: do tests have generated sources?
                this.test = new CompilationUnit(builder.testSourcePaths,
                        builder.testClassesPath, builder.testResourcePaths, builder.testResourcesOutputPath, null);
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

        public PathCollection getSourceParents() {
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

        public void addSourcePathFirst(String path) {
            String absolutePath = Paths.get(path).isAbsolute() ? path
                    : (projectDirectory + File.separator + path);
            this.main.sourcePaths = this.main.sourcePaths.addFirst(Paths.get(absolutePath));
        }

        public static class Builder {

            private ArtifactKey appArtifactKey;
            private String name;
            private String projectDirectory;
            private PathCollection sourcePaths = PathList.of();
            private String classesPath;
            private PathCollection resourcePaths = PathList.of();
            private String resourcesOutputPath;
            private String generatedSourcesPath;

            private String preBuildOutputDir;
            private PathCollection sourceParents = PathList.of();
            private String targetDir;

            private PathCollection testSourcePaths = PathList.of();
            private String testClassesPath;
            private PathCollection testResourcePaths = PathList.of();
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

            public Builder setSourcePaths(PathCollection sourcePaths) {
                this.sourcePaths = sourcePaths;
                return this;
            }

            public Builder setClassesPath(String classesPath) {
                this.classesPath = classesPath;
                return this;
            }

            public Builder setResourcePaths(PathCollection resourcePaths) {
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

            public Builder setSourceParents(PathCollection sourceParents) {
                this.sourceParents = sourceParents;
                return this;
            }

            public Builder setTargetDir(String targetDir) {
                this.targetDir = targetDir;
                return this;
            }

            public Builder setTestSourcePaths(PathCollection testSourcePaths) {
                this.testSourcePaths = testSourcePaths;
                return this;
            }

            public Builder setTestClassesPath(String testClassesPath) {
                this.testClassesPath = testClassesPath;
                return this;
            }

            public Builder setTestResourcePaths(PathCollection testResourcePaths) {
                this.testResourcePaths = testResourcePaths;
                return this;
            }

            public Builder setTestResourcesOutputPath(String testResourcesOutputPath) {
                this.testResourcesOutputPath = testResourcesOutputPath;
                return this;
            }

            public Builder setGeneratedSourcesPath(String generatedSourcesPath) {
                this.generatedSourcesPath = generatedSourcesPath;
                return this;
            }

            public ModuleInfo build() {
                return new ModuleInfo(this);
            }
        }
    }

    public static class CompilationUnit implements Serializable {

        private static final long serialVersionUID = -511238068393954948L;

        private PathCollection sourcePaths;
        private final String classesPath;
        private final PathCollection resourcePaths;
        private final String resourcesOutputPath;
        private final String generatedSourcesPath;

        public CompilationUnit(PathCollection sourcePaths, String classesPath, PathCollection resourcePaths,
                String resourcesOutputPath, String generatedSourcesPath) {
            this.sourcePaths = sourcePaths;
            this.classesPath = classesPath;
            this.resourcePaths = resourcePaths;
            this.resourcesOutputPath = resourcesOutputPath;
            this.generatedSourcesPath = generatedSourcesPath;
        }

        public PathCollection getSourcePaths() {
            return sourcePaths;
        }

        public String getClassesPath() {
            return classesPath;
        }

        public PathCollection getResourcePaths() {
            return resourcePaths;
        }

        public String getResourcesOutputPath() {
            return resourcesOutputPath;
        }

        public String getGeneratedSourcesPath() {
            return generatedSourcesPath;
        }
    }

    public boolean isEnablePreview() {
        if (compilerOptions == null) {
            return false;
        }
        return compilerOptions.getOrDefault("java", Collections.emptySet()).contains(ENABLE_PREVIEW_FLAG);
    }
}

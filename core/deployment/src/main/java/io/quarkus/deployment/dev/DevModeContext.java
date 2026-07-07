package io.quarkus.deployment.dev;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
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

    private static final String ENABLE_PREVIEW_FLAG = "--enable-preview";

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
    private BuildUpdateSource buildUpdateSource = BuildUpdateSource.QUARKUS;
    private ExternalBuildOutputTransport externalBuildOutputTransport = ExternalBuildOutputTransport.disabled();
    private ApplicationModel externalTestApplicationModel;
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

    /**
     * Returns whether Quarkus or an external build tool owns compilation and
     * build-output change detection.
     *
     * @return the update owner, defaulting to {@link BuildUpdateSource#QUARKUS}
     */
    public BuildUpdateSource getBuildUpdateSource() {
        return buildUpdateSource == null ? BuildUpdateSource.QUARKUS : buildUpdateSource;
    }

    /**
     * Selects the owner of compilation and build-output change detection.
     * A {@code null} value restores {@link BuildUpdateSource#QUARKUS}.
     *
     * @param buildUpdateSource update owner
     */
    public void setBuildUpdateSource(BuildUpdateSource buildUpdateSource) {
        this.buildUpdateSource = buildUpdateSource == null ? BuildUpdateSource.QUARKUS : buildUpdateSource;
    }

    /**
     * Returns the authenticated local transport used for externally produced
     * build-output updates.
     *
     * @return transport metadata, disabled by default
     */
    public ExternalBuildOutputTransport getExternalBuildOutputTransport() {
        return externalBuildOutputTransport == null ? ExternalBuildOutputTransport.disabled() : externalBuildOutputTransport;
    }

    /**
     * Configures the authenticated local external-output transport.
     * A {@code null} value disables the transport.
     *
     * @param externalBuildOutputTransport transport metadata
     */
    public void setExternalBuildOutputTransport(ExternalBuildOutputTransport externalBuildOutputTransport) {
        this.externalBuildOutputTransport = externalBuildOutputTransport == null
                ? ExternalBuildOutputTransport.disabled()
                : externalBuildOutputTransport;
    }

    /**
     * Returns the test application model supplied by the external build tool.
     *
     * @return the external test model, or {@code null} when none was supplied
     */
    public ApplicationModel getExternalTestApplicationModel() {
        return externalTestApplicationModel;
    }

    /**
     * Supplies the application model for externally compiled test outputs.
     *
     * @param externalTestApplicationModel external test model, or {@code null}
     *        to clear it
     */
    public void setExternalTestApplicationModel(ApplicationModel externalTestApplicationModel) {
        this.externalTestApplicationModel = externalTestApplicationModel;
    }

    /**
     * Owner of compilation and build-output change detection in dev mode.
     */
    public enum BuildUpdateSource {
        /**
         * Quarkus scans sources and performs compilation through its normal
         * dev-mode pipeline.
         */
        QUARKUS,
        /**
         * The launching build tool compiles sources and sends categorized
         * output changes to Quarkus.
         */
        EXTERNAL_BUILD_TOOL
    }

    /**
     * Serializable launch metadata for the authenticated local external-output
     * transport.
     * <p>
     * The URI and token are validated when a transport connection is created,
     * not when this metadata is populated. The token is sensitive session
     * data. Launching dev mode necessarily serializes this object into its
     * launch context and may place it in the generated dev-runner JAR; that
     * context and JAR must be treated as sensitive, lifecycle-bounded state,
     * and the token must not be logged or persisted elsewhere.
     */
    public static class ExternalBuildOutputTransport implements Serializable {

        private static final long serialVersionUID = 7138938820132266370L;

        private URI uri;
        private String token;

        /**
         * Creates disabled transport metadata.
         *
         * @return metadata with no transport URI
         */
        public static ExternalBuildOutputTransport disabled() {
            return new ExternalBuildOutputTransport();
        }

        /**
         * Creates unvalidated transport metadata. The result is enabled when
         * {@code uri} is non-{@code null}; scheme, address, port, and token
         * validation is deferred until a connection is created.
         *
         * @param uri listener URI, or {@code null} for disabled metadata
         * @param token per-session authentication token, validated later
         * @return transport metadata
         */
        public static ExternalBuildOutputTransport of(URI uri, String token) {
            ExternalBuildOutputTransport transport = new ExternalBuildOutputTransport();
            transport.setUri(uri);
            transport.setToken(token);
            return transport;
        }

        /**
         * Returns whether a transport URI is configured.
         *
         * @return {@code true} when enabled
         */
        public boolean isEnabled() {
            return uri != null;
        }

        /**
         * Returns the configured listener URI.
         *
         * @return listener URI, or empty when disabled
         */
        public Optional<URI> getUri() {
            return Optional.ofNullable(uri);
        }

        /**
         * Sets the listener URI; {@code null} disables the transport.
         *
         * @param uri listener URI
         */
        public void setUri(URI uri) {
            this.uri = uri;
        }

        /**
         * Returns the session authentication token.
         *
         * @return token, or empty when none is configured
         */
        public Optional<String> getToken() {
            return Optional.ofNullable(token);
        }

        /**
         * Sets the session authentication token.
         *
         * @param token token, or {@code null} to clear it
         */
        public void setToken(String token) {
            this.token = token;
        }
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

            /**
             * Sets every main class-output directory, encoded with the current
             * platform path separator for the serialized context.
             *
             * @param classesPaths class-output directories
             * @return this builder
             */
            public Builder setClassesPaths(Collection<Path> classesPaths) {
                this.classesPath = joinPaths(classesPaths);
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

            /**
             * Sets every test class-output directory, encoded with the current
             * platform path separator for the serialized context.
             *
             * @param testClassesPaths test class-output directories
             * @return this builder
             */
            public Builder setTestClassesPaths(Collection<Path> testClassesPaths) {
                this.testClassesPath = joinPaths(testClassesPaths);
                return this;
            }

            private static String joinPaths(Collection<Path> paths) {
                if (paths == null || paths.isEmpty()) {
                    return null;
                }
                return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
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

        /**
         * Returns all class-output directories encoded by
         * {@link ModuleInfo.Builder#setClassesPaths(Collection)} or the legacy
         * single-string setter.
         *
         * @return immutable class-output paths in their encoded order
         */
        public List<Path> getClassesPaths() {
            if (classesPath == null || classesPath.isBlank()) {
                return List.of();
            }
            List<Path> paths = new ArrayList<>();
            for (String path : classesPath.split(Pattern.quote(File.pathSeparator))) {
                if (!path.isBlank()) {
                    paths.add(Path.of(path));
                }
            }
            return List.copyOf(paths);
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

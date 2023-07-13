package io.quarkus.gradle.tasks;

import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.GRADLE_VERSION;
import static io.quarkus.analytics.dto.segment.TrackEventType.DEV_MODE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.inject.Inject;

import org.apache.tools.ant.types.Commandline;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.options.Option;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.gradle.util.GradleVersion;

import io.quarkus.analytics.AnalyticsService;
import io.quarkus.analytics.config.FileLocationsImpl;
import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.ConfiguredClassLoading;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.devmode.DependenciesFilter;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.DevModeMain;
import io.quarkus.deployment.dev.QuarkusDevModeLauncher;
import io.quarkus.gradle.dependency.ApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.dsl.CompilerOption;
import io.quarkus.gradle.dsl.CompilerOptions;
import io.quarkus.gradle.extension.QuarkusPluginExtension;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;

public abstract class QuarkusDev extends QuarkusTask {

    public static final String IO_QUARKUS_DEVMODE_ARGS = "io.quarkus.devmode-args";

    private final Configuration quarkusDevConfiguration;
    private final SourceSet mainSourceSet;

    private final CompilerOptions compilerOptions = new CompilerOptions();

    private final Property<File> workingDirectory;
    private final MapProperty<String, String> environmentVariables;

    private final Property<Boolean> preventNoVerify;
    private final Property<Boolean> shouldPropagateJavaCompilerArgs;
    private final ListProperty<String> args;
    private final ListProperty<String> jvmArgs;

    private final Property<Boolean> openJavaLang;
    private final ListProperty<String> modules;
    private final ListProperty<String> compilerArgs;

    private final Set<File> filesIncludedInClasspath = new HashSet<>();

    @SuppressWarnings("unused")
    @Inject
    public QuarkusDev(Configuration quarkusDevConfiguration, QuarkusPluginExtension extension) {
        this("Development mode: enables hot deployment with background compilation", quarkusDevConfiguration, extension);
    }

    public QuarkusDev(
            String name,
            Configuration quarkusDevConfiguration,
            @SuppressWarnings("unused") QuarkusPluginExtension extension) {
        super(name);
        this.quarkusDevConfiguration = quarkusDevConfiguration;
        mainSourceSet = getProject().getExtensions().getByType(SourceSetContainer.class)
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        final ObjectFactory objectFactory = getProject().getObjects();

        workingDirectory = objectFactory.property(File.class);
        workingDirectory.convention(getProject().provider(() -> QuarkusPluginExtension.getLastFile(getCompilationOutput())));

        environmentVariables = objectFactory.mapProperty(String.class, String.class);

        preventNoVerify = objectFactory.property(Boolean.class);
        preventNoVerify.convention(false);

        shouldPropagateJavaCompilerArgs = objectFactory.property(Boolean.class);
        shouldPropagateJavaCompilerArgs.convention(true);

        args = objectFactory.listProperty(String.class);
        compilerArgs = objectFactory.listProperty(String.class);
        jvmArgs = objectFactory.listProperty(String.class);
        openJavaLang = objectFactory.property(Boolean.class);
        openJavaLang.convention(false);
        modules = objectFactory.listProperty(String.class);
    }

    /**
     * The dependency Configuration associated with this task. Used
     * for up-to-date checks
     */
    @SuppressWarnings("unused")
    @CompileClasspath
    public Configuration getQuarkusDevConfiguration() {
        return this.quarkusDevConfiguration;
    }

    /**
     * The JVM sources (Java, Kotlin, ..) for the project
     */
    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileCollection getSources() {
        return mainSourceSet.getAllJava().getSourceDirectories();
    }

    /**
     * The JVM classes directory (compilation output)
     */
    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileCollection getCompilationOutput() {
        return mainSourceSet.getOutput().getClassesDirs();
    }

    /**
     * The directory to be used as the working dir for the dev process.
     *
     * Defaults to the main source set's classes directory. If there are
     * multiple, one is picked at random (see {@link QuarkusPluginExtension#getLastFile}).
     */
    @Input
    public Property<File> getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * @deprecated See {@link #workingDirectory}
     */
    @Deprecated
    public void setWorkingDir(String workingDir) {
        workingDirectory.set(getProject().file(workingDir));
    }

    @Input
    public MapProperty<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    @Internal
    public Map<String, String> getEnvVars() {
        return environmentVariables.get();
    }

    @Input
    public Property<Boolean> getPreventNoVerify() {
        return preventNoVerify;
    }

    /**
     * @deprecated see {@link #getPreventNoVerify()}
     */
    @SuppressWarnings("SpellCheckingInspection")
    @Deprecated
    @Internal
    public boolean isPreventnoverify() {
        return getPreventNoVerify().get();
    }

    /**
     * @deprecated see {@link #getPreventNoVerify()}
     */
    @SuppressWarnings("SpellCheckingInspection")
    @Deprecated
    @Option(description = "value is intended to be set to true when some generated bytecode is" +
            " erroneous causing the JVM to crash when the verify:none option is set " +
            "(which is on by default)", option = "prevent-noverify")
    public void setPreventnoverify(boolean preventNoVerify) {
        getPreventNoVerify().set(preventNoVerify);
    }

    @Input
    public ListProperty<String> getJvmArguments() {
        return jvmArgs;
    }

    @Internal
    public List<String> getJvmArgs() {
        return jvmArgs.get();
    }

    @SuppressWarnings("unused")
    @Option(description = "Set JVM arguments", option = "jvm-args")
    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs.set(jvmArgs);
    }

    @Input
    public ListProperty<String> getArguments() {
        return args;
    }

    @Option(description = "Modules to add to the application", option = "modules")
    public void setModules(List<String> modules) {
        this.modules.set(modules);
    }

    @Input
    public ListProperty<String> getModules() {
        return modules;
    }

    @Option(description = "Open Java Lang module", option = "open-lang-package")
    public void setOpenJavaLang(Boolean openJavaLang) {
        this.openJavaLang.set(openJavaLang);
    }

    @Input
    public Property<Boolean> getOpenJavaLang() {
        return openJavaLang;
    }

    @SuppressWarnings("unused")
    @Internal
    public List<String> getArgs() {
        return args.get();
    }

    public void setArgs(List<String> args) {
        this.args.set(args);
    }

    @SuppressWarnings("unused")
    @Option(description = "Set application arguments", option = "quarkus-args")
    public void setArgsString(String argsString) {
        this.setArgs(Arrays.asList(Commandline.translateCommandline(argsString)));
    }

    @Input
    public ListProperty<String> getCompilerArguments() {
        return compilerArgs;
    }

    @Internal
    public List<String> getCompilerArgs() {
        return getCompilerArguments().get();
    }

    @SuppressWarnings("unused")
    @Option(description = "Additional parameters to pass to javac when recompiling changed source files", option = "compiler-args")
    public void setCompilerArgs(List<String> compilerArgs) {
        getCompilerArguments().set(compilerArgs);
    }

    @SuppressWarnings("unused")
    @Internal
    public CompilerOptions getCompilerOptions() {
        return this.compilerOptions;
    }

    @SuppressWarnings("unused")
    public QuarkusDev compilerOptions(Action<CompilerOptions> action) {
        action.execute(compilerOptions);
        return this;
    }

    @TaskAction
    public void startDev() {
        if (!sourcesExist()) {
            throw new GradleException("The `src/main/java` directory is required, please create it.");
        }

        if (!classesExist()) {
            throw new GradleException("The project has no output yet, " +
                    "this should not happen as build should have been executed first. " +
                    "Does the project have any source files?");
        }
        AnalyticsService analyticsService = new AnalyticsService(FileLocationsImpl.INSTANCE,
                new GradleMessageWriter(getLogger()));
        analyticsService.buildAnalyticsUserInput((String prompt) -> {
            System.out.print(prompt);
            try (Scanner scanner = new Scanner(new FilterInputStream(System.in) {
                @Override
                public void close() throws IOException {
                    //don't close System.in!
                }
            })) {
                return scanner.nextLine();
            } catch (Exception e) {
                getLogger().warn("Failed to collect user input for analytics", e);
                return "";
            }
        });

        try {
            QuarkusDevModeLauncher runner = newLauncher(analyticsService);
            String outputFile = System.getProperty(IO_QUARKUS_DEVMODE_ARGS);
            if (outputFile == null) {
                getProject().exec(action -> {
                    action.commandLine(runner.args()).workingDir(getWorkingDirectory().get());
                    action.environment(getEnvVars());
                    action.setStandardInput(System.in)
                            .setErrorOutput(System.out)
                            .setStandardOutput(System.out);
                });
            } else {
                try (BufferedWriter is = Files.newBufferedWriter(Paths.get(outputFile))) {
                    for (String i : runner.args()) {
                        is.write(i);
                        is.newLine();
                    }
                }
            }

        } catch (Exception e) {
            throw new GradleException("Failed to run", e);
        } finally {
            analyticsService.close();
        }
    }

    private boolean sourcesExist() {
        final Set<FileSystemLocation> srcDirLocations = mainSourceSet.getAllJava().getSourceDirectories().getElements().get();
        for (FileSystemLocation srcDirLocation : srcDirLocations) {
            final File srcDir = srcDirLocation.getAsFile();
            if (srcDir.exists() && srcDir.isDirectory()) {
                final File[] files = srcDir.listFiles();
                if (files != null && files.length > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean classesExist() {
        for (FileSystemLocation location : getCompilationOutput().getElements().get()) {
            final File locationAsFile = location.getAsFile();
            if (locationAsFile.isDirectory()) {
                return true;
            }
        }
        return false;
    }

    private QuarkusDevModeLauncher newLauncher(final AnalyticsService analyticsService) throws Exception {
        final Project project = getProject();
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);

        String java = null;

        if (GradleVersion.current().compareTo(GradleVersion.version("6.7")) >= 0) {
            JavaToolchainService toolChainService = project.getExtensions().getByType(JavaToolchainService.class);
            JavaToolchainSpec toolchainSpec = javaPluginExtension.getToolchain();
            Provider<JavaLauncher> javaLauncher = toolChainService.launcherFor(toolchainSpec);
            if (javaLauncher.isPresent()) {
                java = javaLauncher.get().getExecutablePath().getAsFile().getAbsolutePath();
            }
        }
        GradleDevModeLauncher.Builder builder = GradleDevModeLauncher.builder(getLogger(), java)
                .preventnoverify(getPreventNoVerify().getOrElse(false))
                .projectDir(projectDir)
                .buildDir(buildDir)
                .outputDir(buildDir)
                .debug(System.getProperty("debug"))
                .debugHost(System.getProperty("debugHost"))
                .debugPort(System.getProperty("debugPort"))
                .suspend(System.getProperty("suspend"));
        if (System.getProperty(IO_QUARKUS_DEVMODE_ARGS) == null) {
            builder.jvmArgs("-Dquarkus.console.basic=true")
                    .jvmArgs("-Dio.quarkus.force-color-support=true");
        }

        if (getJvmArguments().isPresent() && !getJvmArguments().get().isEmpty()) {
            builder.jvmArgs(getJvmArgs());
        }

        if (getOpenJavaLang().isPresent() && getOpenJavaLang().get()) {
            builder.jvmArgs("--add-opens");
            builder.jvmArgs("java.base/java.lang=ALL-UNNAMED");
        }

        if (getModules().isPresent() && !getModules().get().isEmpty()) {
            String mods = String.join(",", getModules().get());
            builder.jvmArgs("--add-modules");
            builder.jvmArgs(mods);
        }

        for (Map.Entry<String, ?> e : project.getProperties().entrySet()) {
            if (e.getValue() instanceof String) {
                builder.buildSystemProperty(e.getKey(), e.getValue().toString());
            }
        }

        //  this is a minor hack to allow ApplicationConfig to be populated with defaults
        builder.applicationName(project.getName());
        builder.applicationVersion(project.getVersion().toString());

        builder.sourceEncoding(getSourceEncoding());

        final ApplicationModel appModel = extension().getApplicationModel(LaunchMode.DEVELOPMENT);

        analyticsService.sendAnalytics(
                DEV_MODE,
                appModel,
                Map.of(GRADLE_VERSION, getProject().getGradle().getGradleVersion()),
                getProject().getBuildDir().getAbsoluteFile());

        final Set<ArtifactKey> projectDependencies = new HashSet<>();
        for (ResolvedDependency localDep : DependenciesFilter.getReloadableModules(appModel)) {
            addLocalProject(localDep, builder, projectDependencies, appModel.getAppArtifact().getWorkspaceModule().getId()
                    .equals(localDep.getWorkspaceModule().getId()));
        }

        addQuarkusDevModeDeps(builder, appModel);

        //look for an application.properties
        Set<Path> resourceDirs = new HashSet<>();
        for (SourceDir resourceDir : appModel.getApplicationModule().getMainSources().getResourceDirs()) {
            resourceDirs.add(resourceDir.getOutputDir());
        }

        final Collection<ArtifactKey> configuredParentFirst = ConfiguredClassLoading.builder()
                .setApplicationModel(appModel)
                .setApplicationRoot(PathsCollection.from(resourceDirs))
                .setMode(QuarkusBootstrap.Mode.DEV)
                .build().getParentFirstArtifacts();

        for (io.quarkus.maven.dependency.ResolvedDependency artifact : appModel.getDependencies()) {
            if (!projectDependencies.contains(artifact.getKey())) {
                artifact.getResolvedPaths().forEach(p -> {
                    File file = p.toFile();
                    if (file.exists() && configuredParentFirst.contains(artifact.getKey())
                            && filesIncludedInClasspath.add(file)) {
                        getLogger().debug("Adding dependency {}", file);
                        builder.classpathEntry(artifact.getKey(), file);
                    }
                });
            }
        }

        builder.sourceJavaVersion(javaPluginExtension.getSourceCompatibility().toString());
        builder.targetJavaVersion(javaPluginExtension.getTargetCompatibility().toString());

        for (CompilerOption compilerOptions : compilerOptions.getCompilerOptions()) {
            builder.compilerOptions(compilerOptions.getName(), compilerOptions.getArgs());
        }

        if (shouldPropagateJavaCompilerArgs.get() && getCompilerArgs().isEmpty()) {
            getJavaCompileTask()
                    .map(compileTask -> compileTask.getOptions().getCompilerArgs())
                    .ifPresent(args -> builder.compilerOptions("java", args));
        } else {
            builder.compilerOptions("java", getCompilerArgs());
        }

        modifyDevModeContext(builder);

        final Path serializedModel = ToolingUtils.serializeAppModel(appModel, this, false);
        serializedModel.toFile().deleteOnExit();
        builder.jvmArgs("-D" + BootstrapConstants.SERIALIZED_APP_MODEL + "=" + serializedModel.toAbsolutePath());

        final ApplicationModel testAppModel = extension().getApplicationModel(LaunchMode.TEST);
        final Path serializedTestModel = ToolingUtils.serializeAppModel(testAppModel, this, true);
        serializedTestModel.toFile().deleteOnExit();
        builder.jvmArgs("-D" + BootstrapConstants.SERIALIZED_TEST_APP_MODEL + "=" + serializedTestModel.toAbsolutePath());

        //        extension().outputDirectory().mkdirs();

        if (args.isPresent() && !args.get().isEmpty()) {
            builder.applicationArgs(String.join(" ", args.get()));
        }

        return builder.build();
    }

    protected void modifyDevModeContext(GradleDevModeLauncher.Builder builder) {

    }

    private void addQuarkusDevModeDeps(GradleDevModeLauncher.Builder builder, ApplicationModel appModel) {

        ResolvedDependency coreDeployment = null;
        for (ResolvedDependency d : appModel.getDependencies()) {
            if (d.isDeploymentCp() && d.getArtifactId().equals("quarkus-core-deployment")
                    && d.getGroupId().equals("io.quarkus")) {
                coreDeployment = d;
                break;
            }
        }
        if (coreDeployment == null) {
            throw new GradleException("Failed to locate io.quarkus:quarkus-core-deployment on the application build classpath");
        }

        final String pomPropsPath = "META-INF/maven/io.quarkus/quarkus-bootstrap-gradle-resolver/pom.properties";
        final InputStream devModePomPropsIs = DevModeMain.class.getClassLoader().getResourceAsStream(pomPropsPath);
        if (devModePomPropsIs == null) {
            throw new GradleException("Failed to locate " + pomPropsPath + " on the classpath");
        }
        final Properties devModeProps = new Properties();
        try (InputStream is = devModePomPropsIs) {
            devModeProps.load(is);
        } catch (IOException e) {
            throw new GradleException("Failed to load " + pomPropsPath + " from the classpath", e);
        }
        final String devModeGroupId = devModeProps.getProperty("groupId");
        if (devModeGroupId == null) {
            throw new GradleException("Classpath resource " + pomPropsPath + " is missing groupId");
        }
        final String devModeArtifactId = devModeProps.getProperty("artifactId");
        if (devModeArtifactId == null) {
            throw new GradleException("Classpath resource " + pomPropsPath + " is missing artifactId");
        }
        final String devModeVersion = devModeProps.getProperty("version");
        if (devModeVersion == null) {
            throw new GradleException("Classpath resource " + pomPropsPath + " is missing version");
        }

        Dependency gradleResolverDep = getProject().getDependencies()
                .create(String.format("%s:%s:%s", devModeGroupId, devModeArtifactId, devModeVersion));
        Dependency coreDeploymentDep = getProject().getDependencies()
                .create(String.format("%s:%s:%s", coreDeployment.getGroupId(), coreDeployment.getArtifactId(),
                        coreDeployment.getVersion()));

        final Configuration devModeDependencyConfiguration = getProject().getConfigurations()
                .detachedConfiguration(gradleResolverDep, coreDeploymentDep);

        final String platformConfigName = ToolingUtils.toPlatformConfigurationName(
                ApplicationDeploymentClasspathBuilder.getFinalRuntimeConfigName(LaunchMode.DEVELOPMENT));
        final Configuration platformConfig = getProject().getConfigurations().findByName(platformConfigName);
        if (platformConfig != null) {
            // apply the platforms
            devModeDependencyConfiguration.extendsFrom(platformConfig);
        }

        for (ResolvedArtifact appDep : devModeDependencyConfiguration.getResolvedConfiguration().getResolvedArtifacts()) {
            ModuleVersionIdentifier artifactId = appDep.getModuleVersion().getId();
            //we only use the launcher for launching from the IDE, we need to exclude it
            if (!(artifactId.getGroup().equals("io.quarkus")
                    && artifactId.getName().equals("quarkus-ide-launcher"))) {
                if (artifactId.getGroup().equals("io.quarkus")
                        && artifactId.getName().equals("quarkus-class-change-agent")) {
                    builder.jvmArgs("-javaagent:" + appDep.getFile().getAbsolutePath());
                } else {
                    builder.classpathEntry(ArtifactKey.of(appDep.getModuleVersion().getId().getGroup(), appDep.getName(),
                            appDep.getClassifier(), appDep.getExtension()), appDep.getFile());
                }
            }
        }
    }

    private void addLocalProject(ResolvedDependency project, GradleDevModeLauncher.Builder builder, Set<ArtifactKey> addeDeps,
            boolean root) {
        addeDeps.add(project.getKey());

        final ArtifactSources sources = project.getSources();
        if (sources == null) {
            return;
        }

        Set<Path> sourcePaths = new LinkedHashSet<>();
        Set<Path> sourceParentPaths = new LinkedHashSet<>();

        final Set<Path> classesDirs = new HashSet<>(sources.getSourceDirs().size());
        for (SourceDir src : sources.getSourceDirs()) {
            if (Files.exists(src.getDir())) {
                sourcePaths.add(src.getDir());
                sourceParentPaths.add(src.getDir().getParent());
                if (src.getOutputDir() != null) {
                    classesDirs.add(src.getOutputDir());
                }
            }
        }
        Path classesDir = classesDirs.isEmpty() ? null
                : QuarkusGradleUtils.mergeClassesDirs(classesDirs, project.getWorkspaceModule().getBuildDir(), root, false);

        final Set<Path> resourcesSrcDirs = new LinkedHashSet<>();
        // resourcesSrcDir may exist but if it's empty the resources output dir won't be created
        Path resourcesOutputDir = null;
        for (SourceDir resource : sources.getResourceDirs()) {
            resourcesSrcDirs.add(resource.getDir());
            if (resourcesOutputDir == null) {
                resourcesOutputDir = resource.getOutputDir();
            }
        }

        if (sourcePaths.isEmpty() && (resourcesOutputDir == null || !Files.exists(resourcesOutputDir)) || classesDir == null) {
            return;
        }

        final String resourcesOutputPath;
        if (resourcesOutputDir != null && Files.exists(resourcesOutputDir)) {
            resourcesOutputPath = resourcesOutputDir.toString();
            if (!Files.exists(classesDir)) {
                // currently classesDir can't be null and is expected to exist
                classesDir = resourcesOutputDir;
            }
        } else {
            // currently resources dir should exist
            resourcesOutputPath = classesDir.toString();
        }

        DevModeContext.ModuleInfo.Builder moduleBuilder = new DevModeContext.ModuleInfo.Builder()
                .setArtifactKey(project.getKey())
                .setName(project.getArtifactId())
                .setProjectDirectory(project.getWorkspaceModule().getModuleDir().getAbsolutePath())
                .setSourcePaths(PathList.from(sourcePaths))
                .setClassesPath(classesDir.toString())
                .setResourcePaths(PathList.from(resourcesSrcDirs))
                .setResourcesOutputPath(resourcesOutputPath)
                .setSourceParents(PathList.from(sourceParentPaths))
                .setPreBuildOutputDir(project.getWorkspaceModule().getBuildDir().toPath().resolve("generated-sources")
                        .toAbsolutePath().toString())
                .setTargetDir(project.getWorkspaceModule().getBuildDir().toString());

        final ArtifactSources testSources = project.getWorkspaceModule().getTestSources();
        if (testSources != null) {
            Set<Path> testSourcePaths = new LinkedHashSet<>();
            Set<Path> testSourceParentPaths = new LinkedHashSet<>();

            final Set<Path> testClassesDirs = new HashSet<>(testSources.getSourceDirs().size());
            for (SourceDir src : testSources.getSourceDirs()) {
                if (Files.exists(src.getDir())) {
                    testSourcePaths.add(src.getDir());
                    testSourceParentPaths.add(src.getDir().getParent());
                    if (src.getOutputDir() != null) {
                        testClassesDirs.add(src.getOutputDir());
                    }
                }
            }
            Path testClassesDir = testClassesDirs.isEmpty() ? null
                    : QuarkusGradleUtils.mergeClassesDirs(testClassesDirs, project.getWorkspaceModule().getBuildDir(), root,
                            root);

            final Set<Path> testResourcesSrcDirs = new LinkedHashSet<>();
            // resourcesSrcDir may exist but if it's empty the resources output dir won't be created
            Path testResourcesOutputDir = null;
            for (SourceDir resource : testSources.getResourceDirs()) {
                testResourcesSrcDirs.add(resource.getDir());
                if (testResourcesOutputDir == null) {
                    testResourcesOutputDir = resource.getOutputDir();
                }
            }

            if (testClassesDir != null && (!testSourcePaths.isEmpty()
                    || (testResourcesOutputDir != null && Files.exists(testResourcesOutputDir)))) {
                final String testResourcesOutputPath;
                if (testResourcesOutputDir != null && Files.exists(testResourcesOutputDir)) {
                    testResourcesOutputPath = testResourcesOutputDir.toString();
                    if (!Files.exists(testClassesDir)) {
                        // currently classesDir can't be null and is expected to exist
                        testClassesDir = testResourcesOutputDir;
                    }
                } else {
                    // currently resources dir should exist
                    testResourcesOutputPath = testClassesDir.toString();
                }
                moduleBuilder.setTestSourcePaths(PathList.from(testSourcePaths))
                        .setTestClassesPath(testClassesDir.toString())
                        .setTestResourcePaths(PathList.from(testResourcesSrcDirs))
                        .setTestResourcesOutputPath(testResourcesOutputPath);
            }
        }

        final DevModeContext.ModuleInfo wsModuleInfo = moduleBuilder.build();
        if (root) {
            builder.mainModule(wsModuleInfo);
        } else {
            builder.dependency(wsModuleInfo);
        }
    }

    private String getSourceEncoding() {
        return getJavaCompileTask()
                .map(javaCompile -> javaCompile.getOptions().getEncoding())
                .orElse(null);
    }

    private java.util.Optional<JavaCompile> getJavaCompileTask() {
        return java.util.Optional
                .ofNullable((JavaCompile) getProject().getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME));
    }

    public void shouldPropagateJavaCompilerArgs(boolean shouldPropagateJavaCompilerArgs) {
        this.shouldPropagateJavaCompilerArgs.set(shouldPropagateJavaCompilerArgs);
    }
}

package io.quarkus.gradle.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.options.Option;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.gradle.util.GradleVersion;

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
import io.quarkus.gradle.dsl.CompilerOption;
import io.quarkus.gradle.dsl.CompilerOptions;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;

public class QuarkusDev extends QuarkusTask {

    public static final String IO_QUARKUS_DEVMODE_ARGS = "io.quarkus.devmode-args";

    private Set<File> filesIncludedInClasspath = new HashSet<>();

    protected Configuration quarkusDevConfiguration;

    private File buildDir;

    private String sourceDir;

    private String workingDir;

    private List<String> jvmArgs;

    private boolean preventnoverify = false;

    private List<String> args = new LinkedList<String>();

    private List<String> compilerArgs = new LinkedList<>();

    private CompilerOptions compilerOptions = new CompilerOptions();

    private boolean shouldPropagateJavaCompilerArgs = true;

    @Inject
    public QuarkusDev() {
        super("Development mode: enables hot deployment with background compilation");
    }

    public QuarkusDev(String name) {
        super(name);
    }

    @CompileClasspath
    public Configuration getQuarkusDevConfiguration() {
        return this.quarkusDevConfiguration;
    }

    public void setQuarkusDevConfiguration(Configuration quarkusDevConfiguration) {
        this.quarkusDevConfiguration = quarkusDevConfiguration;
    }

    @InputDirectory
    @Optional
    public File getBuildDir() {
        if (buildDir == null) {
            buildDir = getProject().getBuildDir();
        }
        return buildDir;
    }

    public void setBuildDir(File buildDir) {
        this.buildDir = buildDir;
    }

    @Optional
    @InputDirectory
    public File getSourceDir() {
        if (sourceDir == null) {
            return extension().sourceDir();
        } else {
            return new File(sourceDir);
        }
    }

    @Option(description = "Set source directory", option = "source-dir")
    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }

    @Input
    // @InputDirectory this breaks kotlin projects, the working dir at this stage will be evaluated to 'classes/java/main' instead of 'classes/kotlin/main'
    public String getWorkingDir() {
        if (workingDir == null) {
            return extension().workingDir().toString();
        } else {
            return workingDir;
        }
    }

    @Option(description = "Set working directory", option = "working-dir")
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    @Optional
    @Input
    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    @Option(description = "Set JVM arguments", option = "jvm-args")
    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    @Optional
    @Input
    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    @Option(description = "Set application arguments", option = "quarkus-args")
    public void setArgsString(String argsString) {
        this.setArgs(Arrays.asList(Commandline.translateCommandline(argsString)));
    }

    @Input
    public boolean isPreventnoverify() {
        return preventnoverify;
    }

    @Option(description = "value is intended to be set to true when some generated bytecode is" +
            " erroneous causing the JVM to crash when the verify:none option is set " +
            "(which is on by default)", option = "prevent-noverify")
    public void setPreventnoverify(boolean preventnoverify) {
        this.preventnoverify = preventnoverify;
    }

    @Optional
    @Input
    public List<String> getCompilerArgs() {
        return compilerArgs;
    }

    @Option(description = "Additional parameters to pass to javac when recompiling changed source files", option = "compiler-args")
    public void setCompilerArgs(List<String> compilerArgs) {
        this.compilerArgs = compilerArgs;
    }

    @Internal
    public CompilerOptions getCompilerOptions() {
        return this.compilerOptions;
    }

    public QuarkusDev compilerOptions(Action<CompilerOptions> action) {
        action.execute(compilerOptions);
        return this;
    }

    @TaskAction
    public void startDev() {
        if (!getSourceDir().isDirectory()) {
            throw new GradleException("The `src/main/java` directory is required, please create it.");
        }

        if (!extension().outputDirectory().isDirectory()) {
            throw new GradleException("The project has no output yet, " +
                    "this should not happen as build should have been executed first. " +
                    "Does the project have any source files?");
        }

        try {
            QuarkusDevModeLauncher runner = newLauncher();
            String outputFile = System.getProperty(IO_QUARKUS_DEVMODE_ARGS);
            if (outputFile == null) {
                getProject().exec(action -> {
                    action.commandLine(runner.args()).workingDir(getWorkingDir());
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
        }
    }

    private QuarkusDevModeLauncher newLauncher() throws Exception {
        final Project project = getProject();

        String java = null;

        if (GradleVersion.current().compareTo(GradleVersion.version("6.7")) >= 0) {
            JavaToolchainService toolChainService = project.getExtensions().getByType(JavaToolchainService.class);
            JavaToolchainSpec toolchainSpec = project.getExtensions().getByType(JavaPluginExtension.class).getToolchain();
            Provider<JavaLauncher> javaLauncher = toolChainService.launcherFor(toolchainSpec);
            if (javaLauncher.isPresent()) {
                java = javaLauncher.get().getExecutablePath().getAsFile().getAbsolutePath();
            }
        }
        GradleDevModeLauncher.Builder builder = GradleDevModeLauncher.builder(getLogger(), java)
                .preventnoverify(isPreventnoverify())
                .projectDir(project.getProjectDir())
                .buildDir(getBuildDir())
                .outputDir(getBuildDir())
                .debug(System.getProperty("debug"))
                .debugHost(System.getProperty("debugHost"))
                .debugPort(System.getProperty("debugPort"))
                .suspend(System.getProperty("suspend"));
        if (System.getProperty(IO_QUARKUS_DEVMODE_ARGS) == null) {
            builder.jvmArgs("-Dquarkus.test.basic-console=true")
                    .jvmArgs("-Dio.quarkus.force-color-support=true");
        }

        if (getJvmArgs() != null) {
            builder.jvmArgs(getJvmArgs());
        }

        for (Map.Entry<String, ?> e : project.getProperties().entrySet()) {
            if (e.getValue() instanceof String) {
                builder.buildSystemProperty(e.getKey(), e.getValue().toString());
            }
        }

        //  this is a minor hack to allow ApplicationConfig to be populated with defaults
        builder.applicationName(project.getName());
        if (project.getVersion() != null) {
            builder.applicationVersion(project.getVersion().toString());
        }

        builder.sourceEncoding(getSourceEncoding());

        final ApplicationModel appModel = extension().getApplicationModel(LaunchMode.DEVELOPMENT);
        final Set<ArtifactKey> projectDependencies = new HashSet<>();
        for (ResolvedDependency localDep : DependenciesFilter.getReloadableModules(appModel)) {
            addLocalProject(localDep, builder, projectDependencies, appModel.getAppArtifact().getWorkspaceModule().getId()
                    .equals(localDep.getWorkspaceModule().getId()));
        }

        addQuarkusDevModeDeps(builder);

        //look for an application.properties
        Set<Path> resourceDirs = new HashSet<>();
        for (SourceDir resourceDir : appModel.getApplicationModule().getMainSources().getResourceDirs()) {
            resourceDirs.add(resourceDir.getOutputDir());
        }

        final Collection<ArtifactKey> configuredParentFirst = ConfiguredClassLoading.builder()
                .setApplicationModel(appModel)
                .setApplicationRoot(PathsCollection.from(resourceDirs))
                .setMode(QuarkusBootstrap.Mode.DEV)
                .addParentFirstArtifacts(appModel.getParentFirst())
                .build().getParentFirstArtifacts();

        for (io.quarkus.maven.dependency.ResolvedDependency artifact : appModel.getDependencies()) {
            if (!projectDependencies.contains(artifact.getKey())) {
                artifact.getResolvedPaths().forEach(p -> {
                    File file = p.toFile();
                    if (file.exists() && configuredParentFirst.contains(artifact.getKey())
                            && filesIncludedInClasspath.add(file)) {
                        getProject().getLogger().debug("Adding dependency {}", file);
                        builder.classpathEntry(file);
                    }
                });
            }
        }

        JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaPluginConvention != null) {
            builder.sourceJavaVersion(javaPluginConvention.getSourceCompatibility().toString());
            builder.targetJavaVersion(javaPluginConvention.getTargetCompatibility().toString());
        }

        for (CompilerOption compilerOptions : compilerOptions.getCompilerOptions()) {
            builder.compilerOptions(compilerOptions.getName(), compilerOptions.getArgs());
        }

        if (getCompilerArgs().isEmpty() && shouldPropagateJavaCompilerArgs) {
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

        extension().outputDirectory().mkdirs();

        if (!args.isEmpty()) {
            builder.applicationArgs(String.join(" ", args));
        }

        return builder.build();
    }

    protected void modifyDevModeContext(GradleDevModeLauncher.Builder builder) {

    }

    private void addQuarkusDevModeDeps(GradleDevModeLauncher.Builder builder) {
        final String pomPropsPath = "META-INF/maven/io.quarkus/quarkus-core-deployment/pom.properties";
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

        Dependency devModeDependency = getProject().getDependencies()
                .create(String.format("%s:%s:%s", devModeGroupId, devModeArtifactId, devModeVersion));

        final Configuration devModeDependencyConfiguration = getProject().getConfigurations()
                .detachedConfiguration(devModeDependency);

        for (ResolvedArtifact appDep : devModeDependencyConfiguration.getResolvedConfiguration().getResolvedArtifacts()) {
            ModuleVersionIdentifier artifactId = appDep.getModuleVersion().getId();
            //we only use the launcher for launching from the IDE, we need to exclude it
            if (!(artifactId.getGroup().equals("io.quarkus")
                    && artifactId.getName().equals("quarkus-ide-launcher"))) {
                if (artifactId.getGroup().equals("io.quarkus")
                        && artifactId.getName().equals("quarkus-class-change-agent")) {
                    builder.jvmArgs("-javaagent:" + appDep.getFile().getAbsolutePath());
                } else {
                    builder.classpathEntry(appDep.getFile());
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
                : QuarkusGradleUtils.mergeClassesDirs(classesDirs, project.getWorkspaceModule().getBuildDir(), root, root);

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
        this.shouldPropagateJavaCompilerArgs = shouldPropagateJavaCompilerArgs;
    }
}

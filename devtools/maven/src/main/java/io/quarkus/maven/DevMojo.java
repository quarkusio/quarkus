package io.quarkus.maven;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.utils.ANSI;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.fusesource.jansi.internal.Kernel32;
import org.fusesource.jansi.internal.WindowsSupport;

import io.quarkus.bootstrap.devmode.DependenciesFilter;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.DevModeMain;
import io.quarkus.deployment.dev.QuarkusDevModeLauncher;
import io.quarkus.maven.MavenDevModeLauncher.Builder;
import io.quarkus.maven.components.MavenVersionEnforcer;
import io.quarkus.maven.utilities.MojoUtils;

/**
 * The dev mojo, that runs a quarkus app in a forked process. A background compilation process is launched and any changes are
 * automatically reflected in your running application.
 * <p>
 * You can use this dev mode in a remote container environment with {@code remote-dev}.
 */
@Mojo(name = "dev", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class DevMojo extends AbstractMojo {

    private static final String EXT_PROPERTIES_PATH = "META-INF/quarkus-extension.properties";

    private static final String KOTLIN_MAVEN_PLUGIN_GA = "org.jetbrains.kotlin:kotlin-maven-plugin";

    /**
     * running any one of these phases means the compile phase will have been run, if these have
     * not been run we manually run compile
     */
    private static final Set<String> POST_COMPILE_PHASES = Set.of(
            "compile",
            "process-classes",
            "generate-test-sources",
            "process-test-sources",
            "generate-test-resources",
            "process-test-resources",
            "test-compile",
            "process-test-classes",
            "test",
            "prepare-package",
            "package",
            "pre-integration-test",
            "integration-test",
            "post-integration-test",
            "verify",
            "install",
            "deploy");

    /**
     * running any one of these phases means the test-compile phase will have been run, if these have
     * not been run we manually run test-compile
     */
    private static final Set<String> POST_TEST_COMPILE_PHASES = Set.of(
            "test-compile",
            "process-test-classes",
            "test",
            "prepare-package",
            "package",
            "pre-integration-test",
            "integration-test",
            "post-integration-test",
            "verify",
            "install",
            "deploy");
    private static final String QUARKUS_GENERATE_CODE_GOAL = "generate-code";

    private static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
    private static final String MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin";
    private static final String MAVEN_RESOURCES_PLUGIN = "maven-resources-plugin";
    private static final String MAVEN_TOOLCHAINS_PLUGIN = "maven-toolchains-plugin";

    private static final String ORG_JETBRAINS_KOTLIN = "org.jetbrains.kotlin";
    private static final String KOTLIN_MAVEN_PLUGIN = "kotlin-maven-plugin";

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * If this server should be started in debug mode. The default is to start in debug mode and listen on
     * port 5005. Whether or not the JVM is suspended waiting for a debugger to be attached,
     * depends on the value of {@link #suspend}.
     * <p>
     * {@code debug} supports the following options:
     * <table>
     * <tr>
     * <td><b>Value</b></td>
     * <td>Effect</td>
     * </tr>
     * <tr>
     * <td><b>false</b></td>
     * <td>The JVM is not started in debug mode</td>
     * </tr>
     * <tr>
     * <td><b>true</b></td>
     * <td>The JVM is started in debug mode and will be listening on {@code debugHost}:{@code debugPort}</td>
     * </tr>
     * <tr>
     * <td><b>client</b></td>
     * <td>The JVM is started in client mode, and will attempt to connect to {@code debugHost}:{@code debugPort}</td>
     * </tr>
     * <tr>
     * <td><b>{port}</b></td>
     * <td>The JVM is started in debug mode and will be listening on {@code debugHost}:{port}.</td>
     * </tr>
     * </table>
     * By default, {@code debugHost} has the value "localhost", and {@code debugPort} is 5005.
     */
    @Parameter(defaultValue = "${debug}")
    private String debug;

    /**
     * Whether or not the JVM launch, in debug mode, should be suspended. This parameter is only
     * relevant when the JVM is launched in {@link #debug debug mode}. This parameter supports the
     * following values (all the allowed values are case insensitive):
     * <table>
     * <th>
     * <td>Value</td>
     * <td>Effect</td>
     * </th>
     * <tr>
     * <td>y or true</td>
     * <td>The debug mode JVM launch is suspended</td>
     * </tr>
     * <tr>
     * <td>n or false</td>
     * <td>The debug mode JVM is started without suspending</td>
     * </tr>
     * </table>
     */
    @Parameter(defaultValue = "${suspend}")
    private String suspend;

    @Parameter(defaultValue = "${debugHost}")
    private String debugHost;

    @Parameter(defaultValue = "${debugPort}")
    private String debugPort;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceDir;

    @Parameter(defaultValue = "${project.build.directory}")
    private File workingDir;

    @Parameter(defaultValue = "${jvm.args}")
    private String jvmArgs;

    @Parameter(defaultValue = "${quarkus.args}")
    private String argsString;

    @Parameter
    private Map<String, String> environmentVariables = Collections.emptyMap();

    @Parameter
    private Map<String, String> systemProperties = Collections.emptyMap();

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Parameter(defaultValue = "TRUE")
    private boolean deleteDevJar;

    @Component
    private MavenVersionEnforcer mavenVersionEnforcer;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true, required = true)
    private List<RemoteRepository> pluginRepos;

    /**
     * This value is intended to be set to true when some generated bytecode
     * is erroneous causing the JVM to crash when the verify:none option is set (which is on by default)
     */
    @Parameter(defaultValue = "${preventnoverify}")
    private boolean preventnoverify = false;

    /**
     * Whether changes in the projects that appear to be dependencies of the project containing the application to be launched
     * should trigger hot-reload. By default they do.
     */
    @Parameter(defaultValue = "${noDeps}")
    private boolean noDeps = false;

    /**
     * Additional parameters to pass to javac when recompiling changed
     * source files.
     */
    @Parameter
    private List<String> compilerArgs;

    /**
     * The -source argument to javac.
     */
    @Parameter(defaultValue = "${maven.compiler.source}")
    private String source;

    /**
     * The -target argument to javac.
     */
    @Parameter(defaultValue = "${maven.compiler.target}")
    private String target;

    /**
     * Whether or not to enforce the quarkus-maven-plugin build goal to be configured.
     * By default, a missing build goal is considered an inconsistency (although the build goal is not <i>required</i>
     * technically).
     * In this case a warning will be logged and the application will not be started.
     */
    @Parameter(defaultValue = "${quarkus.enforceBuildGoal}")
    private boolean enforceBuildGoal = true;

    @Component
    private WorkspaceReader wsReader;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private ToolchainManager toolchainManager;

    private Map<AppArtifactKey, Plugin> pluginMap;

    /**
     * console attributes, used to restore the console state
     */
    private Attributes attributes;
    private int windowsAttributes;
    private boolean windowsAttributesSet;
    private Connection connection;
    private boolean windowsColorSupport;

    @Override
    public void setLog(Log log) {
        super.setLog(log);
        MojoLogger.delegate = log;
    }

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        saveTerminalState();

        mavenVersionEnforcer.ensureMavenVersion(getLog(), session);

        initToolchain();

        //we always want to compile if needed, so if it is run from the parent it will compile dependent projects
        handleAutoCompile();

        if (enforceBuildGoal) {
            Plugin pluginDef = MojoUtils.checkProjectForMavenBuildPlugin(project);

            if (pluginDef == null) {
                getLog().warn("The quarkus-maven-plugin build goal was not configured for this project, " +
                        "skipping quarkus:dev as this is assumed to be a support library. If you want to run quarkus dev" +
                        " on this project make sure the quarkus-maven-plugin is configured with a build goal.");
                return;
            }
        }

        try {

            DevModeRunner runner = new DevModeRunner();
            Map<Path, Long> pomFiles = readPomFileTimestamps(runner);
            runner.run();
            long nextCheck = System.currentTimeMillis() + 100;
            for (;;) {
                //we never suspend after the first run
                suspend = "n";
                long sleep = Math.max(0, nextCheck - System.currentTimeMillis()) + 1;
                Thread.sleep(sleep);
                if (System.currentTimeMillis() > nextCheck) {
                    nextCheck = System.currentTimeMillis() + 100;
                    if (!runner.alive()) {
                        restoreTerminalState();
                        if (!runner.isExpectedExitValue()) {
                            throw new MojoExecutionException("Dev mode process did not complete successfully");
                        }
                        return;
                    }
                    final Set<Path> changed = new HashSet<>();
                    for (Map.Entry<Path, Long> e : pomFiles.entrySet()) {
                        long t = Files.getLastModifiedTime(e.getKey()).toMillis();
                        if (t > e.getValue()) {
                            changed.add(e.getKey());
                            pomFiles.put(e.getKey(), t);
                        }
                    }
                    if (!changed.isEmpty()) {
                        getLog().info("Changes detected to " + changed + ", restarting dev mode");
                        final DevModeRunner newRunner;
                        try {
                            triggerCompile(false);
                            triggerCompile(true);
                            newRunner = new DevModeRunner();
                        } catch (Exception e) {
                            getLog().info("Could not load changed pom.xml file, changes not applied", e);
                            continue;
                        }
                        runner.stop();
                        newRunner.run();
                        runner = newRunner;
                    }
                }

            }

        } catch (Exception e) {
            throw new MojoFailureException("Failed to run", e);
        }
    }

    /**
     * if the process is forcibly killed then the terminal may be left in raw mode, which
     * messes everything up. This attempts to fix that by saving the state so it can be restored
     */
    private void saveTerminalState() {
        try {
            windowsAttributes = WindowsSupport.getConsoleMode();
            windowsAttributesSet = true;
            if (windowsAttributes > 0) {
                long hConsole = Kernel32.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
                if (hConsole != (long) Kernel32.INVALID_HANDLE_VALUE) {
                    final int VIRTUAL_TERMINAL_PROCESSING = 0x0004; //enable color on the windows console
                    if (Kernel32.SetConsoleMode(hConsole, windowsAttributes | VIRTUAL_TERMINAL_PROCESSING) != 0) {
                        windowsColorSupport = true;
                    }
                }
            }
        } catch (Throwable t) {
            try {
                //this does not work on windows
                //jansi creates an input pump thread, that will steal
                //input from the dev mode process
                new TerminalConnection(new Consumer<Connection>() {
                    @Override
                    public void accept(Connection connection) {
                        attributes = connection.getAttributes();
                        DevMojo.this.connection = connection;
                    }
                });
            } catch (IOException e) {
                getLog().error(
                        "Failed to setup console restore, console may be left in an inconsistent state if the process is killed",
                        e);
            }
        }
    }

    private void restoreTerminalState() {
        if (windowsAttributesSet) {
            WindowsSupport.setConsoleMode(windowsAttributes);
        } else {
            if (attributes == null || connection == null) {
                return;
            }
            connection.setAttributes(attributes);
            int height = connection.size().getHeight();
            connection.write(ANSI.MAIN_BUFFER);
            connection.write(ANSI.CURSOR_SHOW);
            connection.write("\u001B[0m");
            connection.write("\033[" + height + ";0H");
            connection.close();
        }
    }

    private void handleAutoCompile() throws MojoExecutionException {
        //we check to see if there was a compile (or later) goal before this plugin
        boolean compileNeeded = true;
        boolean testCompileNeeded = true;
        boolean prepareNeeded = true;
        for (String goal : session.getGoals()) {
            if (goal.endsWith("quarkus:prepare")) {
                prepareNeeded = false;
            }

            if (POST_COMPILE_PHASES.contains(goal)) {
                compileNeeded = false;
                break;
            }
            if (POST_TEST_COMPILE_PHASES.contains(goal)) {
                testCompileNeeded = false;
                break;
            }
            if (goal.endsWith("quarkus:dev")) {
                break;
            }
        }

        //if the user did not compile we run it for them
        if (compileNeeded) {
            if (prepareNeeded) {
                triggerPrepare();
            }
            triggerCompile(false);
        }
        if (testCompileNeeded) {
            try {
                triggerCompile(true);
            } catch (Throwable t) {
                getLog().error("Test compile failed, you will need to fix your tests before you can use continuous testing", t);
            }
        }
    }

    private void initToolchain() throws MojoExecutionException {
        executeIfConfigured(ORG_APACHE_MAVEN_PLUGINS, MAVEN_TOOLCHAINS_PLUGIN, "toolchain");
    }

    private void triggerPrepare() throws MojoExecutionException {
        final PluginDescriptor pluginDescr = (PluginDescriptor) getPluginContext().get("pluginDescriptor");
        executeIfConfigured(pluginDescr.getGroupId(), pluginDescr.getArtifactId(), QUARKUS_GENERATE_CODE_GOAL);
    }

    private void triggerCompile(boolean test) throws MojoExecutionException {
        handleResources(test);

        // compile the Kotlin sources if needed
        executeIfConfigured(ORG_JETBRAINS_KOTLIN, KOTLIN_MAVEN_PLUGIN, test ? "test-compile" : "compile");

        // Compile the Java sources if needed
        executeIfConfigured(ORG_APACHE_MAVEN_PLUGINS, MAVEN_COMPILER_PLUGIN, test ? "testCompile" : "compile");
    }

    /**
     * Execute the resources:resources goal if resources have been configured on the project
     */
    private void handleResources(boolean test) throws MojoExecutionException {
        List<Resource> resources = project.getResources();
        if (resources.isEmpty()) {
            return;
        }
        executeIfConfigured(ORG_APACHE_MAVEN_PLUGINS, MAVEN_RESOURCES_PLUGIN, test ? "testResources" : "resources");
    }

    private void executeIfConfigured(String pluginGroupId, String pluginArtifactId, String goal) throws MojoExecutionException {
        final Plugin plugin = getConfiguredPluginOrNull(pluginGroupId, pluginArtifactId);
        if (plugin == null || plugin.getExecutions().stream().noneMatch(exec -> exec.getGoals().contains(goal))) {
            return;
        }
        getLog().info("Invoking " + plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion() + ":" + goal
                + " @ " + project.getArtifactId());
        executeMojo(
                plugin(
                        groupId(pluginGroupId),
                        artifactId(pluginArtifactId),
                        version(plugin.getVersion()),
                        plugin.getDependencies()),
                goal(goal),
                getPluginConfig(plugin, goal),
                executionEnvironment(
                        project,
                        session,
                        pluginManager));
    }

    private Xpp3Dom getPluginConfig(Plugin plugin, String goal) throws MojoExecutionException {
        Xpp3Dom mergedConfig = null;
        if (!plugin.getExecutions().isEmpty()) {
            for (PluginExecution exec : plugin.getExecutions()) {
                if (exec.getConfiguration() != null && exec.getGoals().contains(goal)) {
                    mergedConfig = mergedConfig == null ? (Xpp3Dom) exec.getConfiguration()
                            : Xpp3Dom.mergeXpp3Dom(mergedConfig, (Xpp3Dom) exec.getConfiguration(), true);
                }
            }
        }

        if ((Xpp3Dom) plugin.getConfiguration() != null) {
            mergedConfig = mergedConfig == null ? (Xpp3Dom) plugin.getConfiguration()
                    : Xpp3Dom.mergeXpp3Dom(mergedConfig, (Xpp3Dom) plugin.getConfiguration(), true);
        }

        final Xpp3Dom configuration = configuration();
        if (mergedConfig != null) {
            Set<String> supportedParams = null;
            // Filter out `test*` configurations
            for (Xpp3Dom child : mergedConfig.getChildren()) {
                if (child.getName().startsWith("test")) {
                    continue;
                }
                if (supportedParams == null) {
                    supportedParams = getMojoDescriptor(plugin, goal).getParameterMap().keySet();
                }
                if (supportedParams.contains(child.getName())) {
                    configuration.addChild(child);
                }
            }
        }

        return configuration;
    }

    private MojoDescriptor getMojoDescriptor(Plugin plugin, String goal) throws MojoExecutionException {
        try {
            return pluginManager.getMojoDescriptor(plugin, goal, pluginRepos, repoSession);
        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Failed to obtain descriptor for Maven plugin " + plugin.getId() + " goal " + goal, e);
        }
    }

    private Plugin getConfiguredPluginOrNull(String groupId, String artifactId) {
        if (pluginMap == null) {
            pluginMap = new HashMap<>();
            // the original plugin keys may include property expressions, so we can't rely on the exact groupId:artifactId keys
            for (Plugin p : project.getBuildPlugins()) {
                pluginMap.put(new AppArtifactKey(p.getGroupId(), p.getArtifactId()), p);
            }
        }
        return pluginMap.get(new AppArtifactKey(groupId, artifactId));
    }

    private Map<Path, Long> readPomFileTimestamps(DevModeRunner runner) throws IOException {
        Map<Path, Long> ret = new HashMap<>();
        for (Path i : runner.pomFiles()) {
            ret.put(i, Files.getLastModifiedTime(i).toMillis());
        }
        return ret;
    }

    private String getSourceEncoding() {
        Object sourceEncodingProperty = project.getProperties().get("project.build.sourceEncoding");
        if (sourceEncodingProperty != null) {
            return (String) sourceEncodingProperty;
        }
        return null;
    }

    private void addProject(MavenDevModeLauncher.Builder builder, LocalProject localProject, boolean root) throws Exception {

        String projectDirectory;
        Set<Path> sourcePaths;
        String classesPath = null;
        Set<Path> resourcePaths;
        Set<Path> testSourcePaths;
        String testClassesPath = null;
        Set<Path> testResourcePaths;
        List<Profile> activeProfiles = Collections.emptyList();

        final MavenProject mavenProject = session.getProjectMap().get(
                String.format("%s:%s:%s", localProject.getGroupId(), localProject.getArtifactId(), localProject.getVersion()));
        if (mavenProject == null) {
            projectDirectory = localProject.getDir().toAbsolutePath().toString();
            Path sourcePath = localProject.getSourcesSourcesDir().toAbsolutePath();
            if (Files.isDirectory(sourcePath)) {
                sourcePaths = Collections.singleton(sourcePath);
            } else {
                sourcePaths = Collections.emptySet();
            }
            Path testSourcePath = localProject.getTestSourcesSourcesDir().toAbsolutePath();
            if (Files.isDirectory(testSourcePath)) {
                testSourcePaths = Collections.singleton(testSourcePath);
            } else {
                testSourcePaths = Collections.emptySet();
            }
        } else {
            projectDirectory = mavenProject.getBasedir().getPath();
            sourcePaths = mavenProject.getCompileSourceRoots().stream()
                    .map(Paths::get)
                    .filter(Files::isDirectory)
                    .map(Path::toAbsolutePath)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            testSourcePaths = mavenProject.getTestCompileSourceRoots().stream()
                    .map(Paths::get)
                    .filter(Files::isDirectory)
                    .map(Path::toAbsolutePath)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            activeProfiles = mavenProject.getActiveProfiles();
        }
        Path sourceParent = localProject.getSourcesDir().toAbsolutePath();

        Path classesDir = localProject.getClassesDir();
        if (Files.isDirectory(classesDir)) {
            classesPath = classesDir.toAbsolutePath().toString();
        }
        Path testClassesDir = localProject.getTestClassesDir();
        if (Files.isDirectory(testClassesDir)) {
            testClassesPath = testClassesDir.toAbsolutePath().toString();
        }
        resourcePaths = localProject.getResourcesSourcesDirs().toList().stream()
                .map(Path::toAbsolutePath)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        testResourcePaths = localProject.getTestResourcesSourcesDirs().toList().stream()
                .map(Path::toAbsolutePath)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // Add the resources and test resources from the profiles
        for (Profile profile : activeProfiles) {
            final BuildBase build = profile.getBuild();
            if (build != null) {
                resourcePaths.addAll(
                        build.getResources().stream()
                                .map(Resource::getDirectory)
                                .map(localProject::resolveRelativeToBaseDir)
                                .map(Path::toAbsolutePath)
                                .collect(Collectors.toList()));
                testResourcePaths.addAll(
                        build.getTestResources().stream()
                                .map(Resource::getDirectory)
                                .map(localProject::resolveRelativeToBaseDir)
                                .map(Path::toAbsolutePath)
                                .collect(Collectors.toList()));
            }
        }

        if (classesPath == null && (!sourcePaths.isEmpty() || !resourcePaths.isEmpty())) {
            throw new MojoExecutionException("Hot reloadable dependency " + localProject.getAppArtifact()
                    + " has not been compiled yet (the classes directory " + classesDir + " does not exist)");
        }

        Path targetDir = Paths.get(project.getBuild().getDirectory());

        DevModeContext.ModuleInfo moduleInfo = new DevModeContext.ModuleInfo.Builder().setAppArtifactKey(localProject.getKey())
                .setName(localProject.getArtifactId())
                .setProjectDirectory(projectDirectory)
                .setSourcePaths(PathsCollection.from(sourcePaths))
                .setClassesPath(classesPath)
                .setResourcesOutputPath(classesPath)
                .setResourcePaths(PathsCollection.from(resourcePaths))
                .setSourceParents(PathsCollection.of(sourceParent.toAbsolutePath()))
                .setPreBuildOutputDir(targetDir.resolve("generated-sources").toAbsolutePath().toString())
                .setTargetDir(targetDir.toAbsolutePath().toString())
                .setTestSourcePaths(PathsCollection.from(testSourcePaths))
                .setTestClassesPath(testClassesPath)
                .setTestResourcesOutputPath(testClassesPath)
                .setTestResourcePaths(PathsCollection.from(testResourcePaths))
                .build();

        if (root) {
            builder.mainModule(moduleInfo);
        } else {
            builder.dependency(moduleInfo);
        }
    }

    private class DevModeRunner {

        final QuarkusDevModeLauncher launcher;
        private Process process;

        private DevModeRunner() throws Exception {
            launcher = newLauncher();
        }

        Collection<Path> pomFiles() {
            return launcher.watchedBuildFiles();
        }

        boolean alive() {
            return process != null && process.isAlive();
        }

        int exitValue() {
            return process == null ? -1 : process.exitValue();
        }

        boolean isExpectedExitValue() {
            // '130' is what the process exits with in remote-dev mode under bash
            return exitValue() == 0 || exitValue() == 130;
        }

        void run() throws Exception {
            // Display the launch command line in dev mode
            if (getLog().isDebugEnabled()) {
                getLog().debug("Launching JVM with command line: " + String.join(" ", launcher.args()));
            }
            final ProcessBuilder processBuilder = new ProcessBuilder(launcher.args())
                    .redirectErrorStream(true)
                    .inheritIO()
                    .directory(workingDir == null ? buildDir : workingDir);
            if (!environmentVariables.isEmpty()) {
                processBuilder.environment().putAll(environmentVariables);
            }
            process = processBuilder.start();

            //https://github.com/quarkusio/quarkus/issues/232
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    process.destroy();
                    try {
                        process.waitFor();
                    } catch (InterruptedException e) {
                        getLog().warn("Unable to properly wait for dev-mode end", e);
                    }
                }
            }, "Development Mode Shutdown Hook"));
        }

        void stop() throws InterruptedException {
            process.destroy();
            process.waitFor();
        }
    }

    private QuarkusDevModeLauncher newLauncher() throws Exception {
        String java = null;
        // See if a toolchain is configured
        if (toolchainManager != null) {
            Toolchain toolchain = toolchainManager.getToolchainFromBuildContext("jdk", session);
            if (toolchain != null) {
                java = toolchain.findTool("java");
                getLog().info("JVM from toolchain: " + java);
            }
        }

        final MavenDevModeLauncher.Builder builder = MavenDevModeLauncher.builder(java, getLog())
                .preventnoverify(preventnoverify)
                .buildDir(buildDir)
                .outputDir(outputDirectory)
                .suspend(suspend)
                .debug(debug)
                .debugHost(debugHost)
                .debugPort(debugPort)
                .deleteDevJar(deleteDevJar);

        setJvmArgs(builder);
        if (windowsColorSupport) {
            builder.jvmArgs("-Dio.quarkus.force-color-support=true");
        }

        builder.projectDir(project.getFile().getParentFile());
        builder.buildSystemProperties((Map) project.getProperties());

        builder.applicationName(project.getArtifactId());
        builder.applicationVersion(project.getVersion());

        builder.sourceEncoding(getSourceEncoding());

        // Set compilation flags.  Try the explicitly given configuration first.  Otherwise,
        // refer to the configuration of the Maven Compiler Plugin.
        final Optional<Xpp3Dom> compilerPluginConfiguration = findCompilerPluginConfiguration();
        if (compilerArgs != null) {
            builder.compilerOptions(compilerArgs);
        } else if (compilerPluginConfiguration.isPresent()) {
            final Xpp3Dom compilerPluginArgsConfiguration = compilerPluginConfiguration.get().getChild("compilerArgs");
            if (compilerPluginArgsConfiguration != null) {
                List<String> compilerPluginArgs = new ArrayList<>();
                for (Xpp3Dom argConfiguration : compilerPluginArgsConfiguration.getChildren()) {
                    compilerPluginArgs.add(argConfiguration.getValue());
                }
                // compilerArgs can also take a value without using arg
                if (compilerPluginArgsConfiguration.getValue() != null
                        && !compilerPluginArgsConfiguration.getValue().isEmpty()) {
                    compilerPluginArgs.add(compilerPluginArgsConfiguration.getValue().trim());
                }
                builder.compilerOptions(compilerPluginArgs);
            }
        }
        if (source != null) {
            builder.sourceJavaVersion(source);
        } else if (compilerPluginConfiguration.isPresent()) {
            final Xpp3Dom javacSourceVersion = compilerPluginConfiguration.get().getChild("source");
            if (javacSourceVersion != null && javacSourceVersion.getValue() != null
                    && !javacSourceVersion.getValue().trim().isEmpty()) {
                builder.sourceJavaVersion(javacSourceVersion.getValue().trim());
            }
        }
        if (target != null) {
            builder.targetJavaVersion(target);
        } else if (compilerPluginConfiguration.isPresent()) {
            final Xpp3Dom javacTargetVersion = compilerPluginConfiguration.get().getChild("target");
            if (javacTargetVersion != null && javacTargetVersion.getValue() != null
                    && !javacTargetVersion.getValue().trim().isEmpty()) {
                builder.targetJavaVersion(javacTargetVersion.getValue().trim());
            }
        }

        setKotlinSpecificFlags(builder);
        if (noDeps) {
            final LocalProject localProject = LocalProject.load(project.getModel().getPomFile().toPath());
            addProject(builder, localProject, true);
            builder.watchedBuildFile(localProject.getRawModel().getPomFile().toPath());
            builder.localArtifact(new AppArtifactKey(localProject.getGroupId(), localProject.getArtifactId(), null, "jar"));
        } else {
            final LocalProject localProject = LocalProject.loadWorkspace(project.getModel().getPomFile().toPath());
            for (LocalProject project : DependenciesFilter.filterNotReloadableDependencies(localProject,
                    this.project.getArtifacts(), repoSystem, repoSession, repos)) {
                addProject(builder, project, project == localProject);
                builder.watchedBuildFile(project.getRawModel().getPomFile().toPath());
                builder.localArtifact(new AppArtifactKey(project.getGroupId(), project.getArtifactId(), null, "jar"));
            }
        }

        addQuarkusDevModeDeps(builder);

        //in most cases these are not used, however they need to be present for some
        //parent-first cases such as logging
        //first we go through and get all the parent first artifacts
        Set<AppArtifactKey> parentFirstArtifacts = new HashSet<>();
        for (Artifact appDep : project.getArtifacts()) {
            if (appDep.getArtifactHandler().getExtension().equals("jar") && appDep.getFile().isFile()) {
                try (ZipFile file = new ZipFile(appDep.getFile())) {
                    ZipEntry entry = file.getEntry(EXT_PROPERTIES_PATH);
                    if (entry != null) {
                        Properties p = new Properties();
                        try (InputStream inputStream = file.getInputStream(entry)) {
                            p.load(inputStream);
                            String parentFirst = p.getProperty(AppModel.PARENT_FIRST_ARTIFACTS);
                            if (parentFirst != null) {
                                String[] artifacts = parentFirst.split(",");
                                for (String artifact : artifacts) {
                                    parentFirstArtifacts.add(new AppArtifactKey(artifact.split(":")));
                                }
                            }
                        }

                    }
                }
            }
        }
        for (Artifact appDep : project.getArtifacts()) {
            // only add the artifact if it's present in the dev mode context
            // we need this to avoid having jars on the classpath multiple times
            AppArtifactKey key = new AppArtifactKey(appDep.getGroupId(), appDep.getArtifactId(),
                    appDep.getClassifier(), appDep.getArtifactHandler().getExtension());
            if (!builder.isLocal(key) && parentFirstArtifacts.contains(key)) {
                builder.classpathEntry(appDep.getFile());
            }
        }

        builder.baseName(project.getBuild().getFinalName());

        modifyDevModeContext(builder);

        if (argsString != null) {
            builder.applicationArgs(argsString);
        }
        propagateUserProperties(builder);

        return builder.build();
    }

    private void setJvmArgs(Builder builder) throws Exception {
        String jvmArgs = this.jvmArgs;
        if (!systemProperties.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            if (jvmArgs != null) {
                buf.append(jvmArgs);
            }
            for (Map.Entry<String, String> prop : systemProperties.entrySet()) {
                buf.append(" -D").append(prop.getKey()).append("=\"").append(prop.getValue()).append("\"");
            }
            jvmArgs = buf.toString();
        }
        if (jvmArgs != null) {
            builder.jvmArgs(Arrays.asList(CommandLineUtils.translateCommandline(jvmArgs)));
        }

    }

    private void propagateUserProperties(MavenDevModeLauncher.Builder builder) {
        Properties userProps = BootstrapMavenOptions.newInstance().getSystemProperties();
        if (userProps == null) {
            return;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("-D");
        for (Object o : userProps.keySet()) {
            String name = o.toString();
            final String value = userProps.getProperty(name);
            buf.setLength(2);
            buf.append(name);
            if (value != null && !value.isEmpty()) {
                buf.append('=');
                buf.append(value);
            }
            builder.jvmArgs(buf.toString());
        }
    }

    private void addQuarkusDevModeDeps(MavenDevModeLauncher.Builder builder)
            throws MojoExecutionException, DependencyResolutionException {
        final String pomPropsPath = "META-INF/maven/io.quarkus/quarkus-core-deployment/pom.properties";
        final InputStream devModePomPropsIs = DevModeMain.class.getClassLoader().getResourceAsStream(pomPropsPath);
        if (devModePomPropsIs == null) {
            throw new MojoExecutionException("Failed to locate " + pomPropsPath + " on the classpath");
        }
        final Properties devModeProps = new Properties();
        try (InputStream is = devModePomPropsIs) {
            devModeProps.load(is);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load " + pomPropsPath + " from the classpath", e);
        }
        final String devModeGroupId = devModeProps.getProperty("groupId");
        if (devModeGroupId == null) {
            throw new MojoExecutionException("Classpath resource " + pomPropsPath + " is missing groupId");
        }
        final String devModeArtifactId = devModeProps.getProperty("artifactId");
        if (devModeArtifactId == null) {
            throw new MojoExecutionException("Classpath resource " + pomPropsPath + " is missing artifactId");
        }
        final String devModeVersion = devModeProps.getProperty("version");
        if (devModeVersion == null) {
            throw new MojoExecutionException("Classpath resource " + pomPropsPath + " is missing version");
        }

        final DefaultArtifact devModeJar = new DefaultArtifact(devModeGroupId, devModeArtifactId, "jar", devModeVersion);
        final DependencyResult cpRes = repoSystem.resolveDependencies(repoSession,
                new DependencyRequest()
                        .setCollectRequest(
                                new CollectRequest()
                                        .setRoot(new org.eclipse.aether.graph.Dependency(devModeJar, JavaScopes.RUNTIME))
                                        .setRepositories(repos)));

        for (ArtifactResult appDep : cpRes.getArtifactResults()) {
            //we only use the launcher for launching from the IDE, we need to exclude it
            if (!(appDep.getArtifact().getGroupId().equals("io.quarkus")
                    && appDep.getArtifact().getArtifactId().equals("quarkus-ide-launcher"))) {
                if (appDep.getArtifact().getGroupId().equals("io.quarkus")
                        && appDep.getArtifact().getArtifactId().equals("quarkus-class-change-agent")) {
                    builder.jvmArgs("-javaagent:" + appDep.getArtifact().getFile().getAbsolutePath());
                } else {
                    builder.classpathEntry(appDep.getArtifact().getFile());
                }
            }
        }
    }

    private void setKotlinSpecificFlags(MavenDevModeLauncher.Builder builder) {
        Plugin kotlinMavenPlugin = null;
        for (Plugin plugin : project.getBuildPlugins()) {
            if (plugin.getKey().equals(KOTLIN_MAVEN_PLUGIN_GA)) {
                kotlinMavenPlugin = plugin;
                break;
            }
        }

        if (kotlinMavenPlugin == null) {
            return;
        }

        getLog().debug("Kotlin Maven plugin detected");

        List<String> compilerPluginArtifacts = new ArrayList<>();
        List<Dependency> dependencies = kotlinMavenPlugin.getDependencies();
        for (Dependency dependency : dependencies) {
            try {
                ArtifactResult resolvedArtifact = repoSystem.resolveArtifact(repoSession,
                        new ArtifactRequest()
                                .setArtifact(new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
                                        dependency.getClassifier(), dependency.getType(), dependency.getVersion()))
                                .setRepositories(repos));

                compilerPluginArtifacts.add(resolvedArtifact.getArtifact().getFile().toPath().toAbsolutePath().toString());
            } catch (ArtifactResolutionException e) {
                getLog().warn("Unable to properly setup dev-mode for Kotlin", e);
                return;
            }
        }
        builder.compilerPluginArtifacts(compilerPluginArtifacts);

        List<String> options = new ArrayList<>();
        Xpp3Dom compilerPluginConfiguration = (Xpp3Dom) kotlinMavenPlugin.getConfiguration();
        if (compilerPluginConfiguration != null) {
            Xpp3Dom compilerPluginArgsConfiguration = compilerPluginConfiguration.getChild("pluginOptions");
            if (compilerPluginArgsConfiguration != null) {
                for (Xpp3Dom argConfiguration : compilerPluginArgsConfiguration.getChildren()) {
                    options.add(argConfiguration.getValue());
                }
            }
        }
        builder.compilerPluginOptions(options);
    }

    protected void modifyDevModeContext(MavenDevModeLauncher.Builder builder) {

    }

    private Optional<Xpp3Dom> findCompilerPluginConfiguration() {
        for (final Plugin plugin : project.getBuildPlugins()) {
            if (!plugin.getKey().equals("org.apache.maven.plugins:maven-compiler-plugin")) {
                continue;
            }
            final Xpp3Dom compilerPluginConfiguration = (Xpp3Dom) plugin.getConfiguration();
            if (compilerPluginConfiguration != null) {
                return Optional.of(compilerPluginConfiguration);
            }
        }
        return Optional.empty();
    }
}

package io.quarkus.maven;

import static java.util.function.Predicate.not;
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
import java.nio.charset.StandardCharsets;
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

import org.aesh.readline.terminal.impl.ExecPty;
import org.aesh.readline.terminal.impl.Pty;
import org.aesh.terminal.Attributes;
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
import org.eclipse.aether.impl.RemoteRepositoryManager;
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

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.devmode.DependenciesFilter;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.DevModeMain;
import io.quarkus.deployment.dev.QuarkusDevModeLauncher;
import io.quarkus.maven.MavenDevModeLauncher.Builder;
import io.quarkus.maven.components.MavenVersionEnforcer;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.runtime.LaunchMode;

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
    private static final String QUARKUS_GENERATE_CODE_TESTS_GOAL = "generate-code-tests";

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

    @Parameter
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

    @Component
    RemoteRepositoryManager remoteRepositoryManager;

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
     * The --release argument to javac.
     */
    @Parameter(defaultValue = "${maven.compiler.release}")
    private String release;

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

    @Component
    protected QuarkusBootstrapProvider bootstrapProvider;

    /**
     * console attributes, used to restore the console state
     */
    private Attributes attributes;
    private int windowsAttributes;
    private boolean windowsAttributesSet;
    private Pty pty;
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
            final PluginDescriptor pluginDescr = getPluginDescriptor();
            final Plugin pluginDef = getConfiguredPluginOrNull(pluginDescr.getGroupId(), pluginDescr.getArtifactId());
            if (pluginDef == null || !isGoalConfigured(pluginDef, "build")) {
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
                            triggerCompile(false, false);
                            triggerCompile(true, false);
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
            //this only works with a proper PTY based terminal
            //Aesh creates an input pump thread, that will steal
            //input from the dev mode process
            try {
                Pty pty = ExecPty.current();
                attributes = pty.getAttr();
                DevMojo.this.pty = pty;
            } catch (Exception e) {
                getLog().debug("Failed to get a local tty", e);
            }
        }
    }

    private void restoreTerminalState() {
        if (windowsAttributesSet) {
            WindowsSupport.setConsoleMode(windowsAttributes);
        } else {
            if (attributes == null || pty == null) {
                return;
            }
            Pty finalPty = pty;
            try (finalPty) {
                finalPty.setAttr(attributes);
                int height = finalPty.getSize().getHeight();
                String sb = ANSI.MAIN_BUFFER +
                        ANSI.CURSOR_SHOW +
                        "\u001B[0m" +
                        "\033[" + height + ";0H";
                finalPty.getSlaveOutput().write(sb.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                getLog().error("Error restoring console state", e);
            }
        }
    }

    private void handleAutoCompile() throws MojoExecutionException {
        //we check to see if there was a compile (or later) goal before this plugin
        boolean compileNeeded = true;
        boolean testCompileNeeded = true;
        boolean prepareNeeded = true;
        boolean prepareTestsNeeded = true;
        for (String goal : session.getGoals()) {
            if (goal.endsWith("quarkus:generate-code")) {
                prepareNeeded = false;
            }
            if (goal.endsWith("quarkus:generate-code-tests")) {
                prepareTestsNeeded = false;
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
            triggerCompile(false, prepareNeeded);
        }
        if (testCompileNeeded) {
            try {
                triggerCompile(true, prepareTestsNeeded);
            } catch (Throwable t) {
                getLog().error("Test compile failed, you will need to fix your tests before you can use continuous testing", t);
            }
        }
    }

    private void initToolchain() throws MojoExecutionException {
        executeIfConfigured(ORG_APACHE_MAVEN_PLUGINS, MAVEN_TOOLCHAINS_PLUGIN, "toolchain", Collections.emptyMap());
    }

    private void triggerPrepare(boolean test) throws MojoExecutionException {
        final PluginDescriptor pluginDescr = getPluginDescriptor();
        executeIfConfigured(pluginDescr.getGroupId(), pluginDescr.getArtifactId(),
                test ? QUARKUS_GENERATE_CODE_TESTS_GOAL : QUARKUS_GENERATE_CODE_GOAL,
                Collections.singletonMap("mode", LaunchMode.DEVELOPMENT.name()));
    }

    private PluginDescriptor getPluginDescriptor() {
        return (PluginDescriptor) getPluginContext().get("pluginDescriptor");
    }

    private void triggerCompile(boolean test, boolean prepareNeeded) throws MojoExecutionException {
        handleResources(test);

        if (prepareNeeded) {
            triggerPrepare(test);
        }

        // compile the Kotlin sources if needed
        executeIfConfigured(ORG_JETBRAINS_KOTLIN, KOTLIN_MAVEN_PLUGIN, test ? "test-compile" : "compile",
                Collections.emptyMap());

        // Compile the Java sources if needed
        executeIfConfigured(ORG_APACHE_MAVEN_PLUGINS, MAVEN_COMPILER_PLUGIN, test ? "testCompile" : "compile",
                Collections.emptyMap());
    }

    /**
     * Execute the resources:resources goal if resources have been configured on the project
     */
    private void handleResources(boolean test) throws MojoExecutionException {
        List<Resource> resources = project.getResources();
        if (resources.isEmpty()) {
            return;
        }
        executeIfConfigured(ORG_APACHE_MAVEN_PLUGINS, MAVEN_RESOURCES_PLUGIN, test ? "testResources" : "resources",
                Collections.emptyMap());
    }

    private void executeIfConfigured(String pluginGroupId, String pluginArtifactId, String goal, Map<String, String> params)
            throws MojoExecutionException {
        final Plugin plugin = getConfiguredPluginOrNull(pluginGroupId, pluginArtifactId);
        if (!isGoalConfigured(plugin, goal)) {
            return;
        }
        getLog().info("Invoking " + plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion() + ":" + goal
                + ") @ " + project.getArtifactId());
        executeMojo(
                plugin(
                        groupId(pluginGroupId),
                        artifactId(pluginArtifactId),
                        version(plugin.getVersion()),
                        plugin.getDependencies()),
                goal(goal),
                getPluginConfig(plugin, goal, params),
                executionEnvironment(
                        project,
                        session,
                        pluginManager));
    }

    public boolean isGoalConfigured(Plugin plugin, String goal) {
        if (plugin == null) {
            return false;
        }
        for (PluginExecution pluginExecution : plugin.getExecutions()) {
            if (pluginExecution.getGoals().contains(goal)) {
                return true;
            }
        }
        return false;
    }

    private Xpp3Dom getPluginConfig(Plugin plugin, String goal, Map<String, String> params) throws MojoExecutionException {
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

        for (Map.Entry<String, String> param : params.entrySet()) {
            final Xpp3Dom p = new Xpp3Dom(param.getKey());
            p.setValue(param.getValue());
            configuration.addChild(p);
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

    private void addProject(MavenDevModeLauncher.Builder builder, WorkspaceModule module, boolean root) throws Exception {

        String projectDirectory;
        Set<Path> sourcePaths;
        String classesPath = null;
        Set<Path> resourcePaths;
        Set<Path> testSourcePaths;
        String testClassesPath;
        Set<Path> testResourcePaths;
        List<Profile> activeProfiles = Collections.emptyList();

        final MavenProject mavenProject = session.getProjectMap().get(
                String.format("%s:%s:%s", module.getId().getGroupId(), module.getId().getArtifactId(),
                        module.getId().getVersion()));
        if (mavenProject == null) {
            projectDirectory = module.getModuleDir().getAbsolutePath();
            sourcePaths = module.getMainSources().stream().map(src -> src.getSourceDir().toPath().toAbsolutePath())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            testSourcePaths = module.getTestSources().stream().map(src -> src.getSourceDir().toPath().toAbsolutePath())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            projectDirectory = mavenProject.getBasedir().getPath();
            sourcePaths = mavenProject.getCompileSourceRoots().stream()
                    .map(Paths::get)
                    .map(Path::toAbsolutePath)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            testSourcePaths = mavenProject.getTestCompileSourceRoots().stream()
                    .map(Paths::get)
                    .map(Path::toAbsolutePath)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            activeProfiles = mavenProject.getActiveProfiles();
        }

        final Path sourceParent;
        if (module.getMainSources().isEmpty()) {
            if (module.getMainResources().isEmpty()) {
                throw new MojoExecutionException("The project does not appear to contain any sources or resources");
            }
            sourceParent = module.getMainResources().iterator().next().getSourceDir().toPath().toAbsolutePath().getParent();
        } else {
            sourceParent = module.getMainSources().iterator().next().getSourceDir().toPath().toAbsolutePath().getParent();
        }

        Path classesDir = module.getMainSources().iterator().next().getDestinationDir().toPath().toAbsolutePath();
        if (Files.isDirectory(classesDir)) {
            classesPath = classesDir.toString();
        }
        Path testClassesDir = module.getTestSources().iterator().next().getDestinationDir().toPath().toAbsolutePath();
        testClassesPath = testClassesDir.toString();

        resourcePaths = module.getMainResources().stream().map(src -> src.getSourceDir().toPath().toAbsolutePath())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        testResourcePaths = module.getTestResources().stream().map(src -> src.getSourceDir().toPath().toAbsolutePath())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // Add the resources and test resources from the profiles
        for (Profile profile : activeProfiles) {
            final BuildBase build = profile.getBuild();
            if (build != null) {
                resourcePaths.addAll(
                        build.getResources().stream()
                                .map(Resource::getDirectory)
                                .map(Paths::get)
                                .map(Path::toAbsolutePath)
                                .collect(Collectors.toList()));
                testResourcePaths.addAll(
                        build.getTestResources().stream()
                                .map(Resource::getDirectory)
                                .map(Paths::get)
                                .map(Path::toAbsolutePath)
                                .collect(Collectors.toList()));
            }
        }

        if (classesPath == null && (!sourcePaths.isEmpty() || !resourcePaths.isEmpty())) {
            throw new MojoExecutionException("Hot reloadable dependency " + module.getId()
                    + " has not been compiled yet (the classes directory " + classesDir + " does not exist)");
        }

        Path targetDir = Paths.get(project.getBuild().getDirectory());

        DevModeContext.ModuleInfo moduleInfo = new DevModeContext.ModuleInfo.Builder()
                .setArtifactKey(new GACT(module.getId().getGroupId(), module.getId().getArtifactId()))
                .setName(module.getId().getArtifactId())
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
                    .directory(workingDir == null ? project.getBasedir() : workingDir);
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
        if (release != null) {
            builder.releaseJavaVersion(release);
        } else if (compilerPluginConfiguration.isPresent()) {
            applyCompilerFlag(compilerPluginConfiguration, "release", builder::releaseJavaVersion);
        }
        if (source != null) {
            builder.sourceJavaVersion(source);
        } else if (compilerPluginConfiguration.isPresent()) {
            applyCompilerFlag(compilerPluginConfiguration, "source", builder::sourceJavaVersion);
        }
        if (target != null) {
            builder.targetJavaVersion(target);
        } else if (compilerPluginConfiguration.isPresent()) {
            applyCompilerFlag(compilerPluginConfiguration, "target", builder::targetJavaVersion);
        }

        setKotlinSpecificFlags(builder);

        // path to the serialized application model
        final Path appModelLocation = resolveSerializedModelLocation();

        ApplicationModel appModel = bootstrapProvider
                .getResolvedApplicationModel(QuarkusBootstrapProvider.getProjectId(project), LaunchMode.DEVELOPMENT);
        if (appModel != null) {
            bootstrapProvider.close();
        } else {
            final MavenArtifactResolver.Builder resolverBuilder = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRemoteRepositories(repos)
                    .setRemoteRepositoryManager(remoteRepositoryManager)
                    .setWorkspaceDiscovery(true);

            // if it already exists, it may be a reload triggered by a change in a POM
            // in which case we should not be using the original Maven session
            boolean reinitializeMavenSession = Files.exists(appModelLocation);
            if (reinitializeMavenSession) {
                Files.delete(appModelLocation);
            } else {
                // we can re-use the original Maven session
                resolverBuilder.setRepositorySystemSession(repoSession);
            }

            appModel = new BootstrapAppModelResolver(resolverBuilder.build())
                    .setDevMode(true)
                    .setCollectReloadableDependencies(!noDeps)
                    .resolveModel(new GACTV(project.getGroupId(), project.getArtifactId(), null, "jar", project.getVersion()));
        }

        // serialize the app model to avoid re-resolving it in the dev process
        BootstrapUtils.serializeAppModel(appModel, appModelLocation);
        builder.jvmArgs("-D" + BootstrapConstants.SERIALIZED_APP_MODEL + "=" + appModelLocation);

        if (noDeps) {
            addProject(builder, appModel.getApplicationModule(), true);
            appModel.getApplicationModule().getBuildFiles().forEach(p -> builder.watchedBuildFile(p));
            builder.localArtifact(new AppArtifactKey(project.getGroupId(), project.getArtifactId()));
        } else {
            for (WorkspaceModule project : DependenciesFilter.getReloadableModules(appModel)) {
                addProject(builder, project, project == appModel.getApplicationModule());
                project.getBuildFiles().forEach(p -> builder.watchedBuildFile(p));
                builder.localArtifact(new AppArtifactKey(project.getId().getGroupId(), project.getId().getArtifactId()));
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
                            String parentFirst = p.getProperty(ApplicationModel.PARENT_FIRST_ARTIFACTS);
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

    private void applyCompilerFlag(Optional<Xpp3Dom> compilerPluginConfiguration, String flagName,
            Consumer<String> builderCall) {
        compilerPluginConfiguration
                .map(cfg -> cfg.getChild(flagName))
                .map(Xpp3Dom::getValue)
                .map(String::trim)
                .filter(not(String::isEmpty))
                .ifPresent(builderCall);
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

    private Path resolveSerializedModelLocation() {
        final Path p = BootstrapUtils.resolveSerializedAppModelPath(Paths.get(project.getBuild().getDirectory()));
        p.toFile().deleteOnExit();
        return p;
    }
}

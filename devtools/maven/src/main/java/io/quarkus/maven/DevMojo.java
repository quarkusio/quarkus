package io.quarkus.maven;

import static io.quarkus.analytics.dto.segment.TrackEventType.DEV_MODE;
import static io.quarkus.maven.QuarkusBootstrapMojo.CLOSE_BOOTSTRAPPED_APP_PARAM;
import static io.quarkus.maven.QuarkusBootstrapMojo.MODE_PARAM;
import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;
import static java.util.Collections.emptyMap;
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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.aesh.readline.terminal.impl.ExecPty;
import org.aesh.readline.terminal.impl.Pty;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.utils.ANSI;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
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
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Exclusion;
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

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.ConfiguredClassLoading;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.devmode.DependenciesFilter;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContextConfig;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.deployment.dev.DevModeCommandLine;
import io.quarkus.deployment.dev.DevModeCommandLineBuilder;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.DevModeMain;
import io.quarkus.deployment.dev.ExtensionDevModeJvmOptionFilter;
import io.quarkus.maven.components.CompilerOptions;
import io.quarkus.maven.components.MavenVersionEnforcer;
import io.quarkus.maven.components.QuarkusWorkspaceProvider;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.expression.Expression;

/**
 * The dev mojo, that runs a quarkus app in a forked process. A background compilation process is launched and any changes are
 * automatically reflected in your running application.
 * <p>
 * You can use this dev mode in a remote container environment with {@code remote-dev}.
 */
@Mojo(name = "dev", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class DevMojo extends AbstractMojo {

    private static final Set<String> IGNORED_PHASES = Set.of(
            "pre-clean", "clean", "post-clean");

    private static final List<String> PRE_DEV_MODE_PHASES = List.of(
            "validate",
            "initialize",
            "generate-sources",
            "process-sources",
            "generate-resources",
            "process-resources",
            "compile",
            "process-classes",
            "generate-test-sources",
            "process-test-sources",
            "generate-test-resources",
            "process-test-resources",
            "test-compile");

    private static final String IO_QUARKUS = "io.quarkus";
    private static final String QUARKUS_GENERATE_CODE_GOAL = "generate-code";
    private static final String QUARKUS_GENERATE_CODE_TESTS_GOAL = "generate-code-tests";

    private static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
    private static final String MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin";
    private static final String MAVEN_RESOURCES_PLUGIN = "maven-resources-plugin";
    private static final String MAVEN_SUREFIRE_PLUGIN = "maven-surefire-plugin";
    private static final String MAVEN_TOOLCHAINS_PLUGIN = "maven-toolchains-plugin";

    private static final String ORG_JETBRAINS_KOTLIN = "org.jetbrains.kotlin";
    private static final String KOTLIN_MAVEN_PLUGIN = "kotlin-maven-plugin";

    private static final String BOOTSTRAP_ID = "DevMojo";

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * If this server should be started in debug mode. The default is to start in debug mode and listen on
     * port 5005. Whether the JVM is suspended waiting for a debugger to be attached,
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

    @Parameter(defaultValue = "${open-lang-package}")
    private boolean openJavaLang;

    /**
     * Allows configuring the modules to add to the application.
     * The listed modules will be added using: {@code --add-modules m1,m2...}.
     */
    @Parameter(defaultValue = "${add-modules}")
    private List<String> modules;

    /**
     * Whether the JVM launch, in debug mode, should be suspended. This parameter is only
     * relevant when the JVM is launched in {@link #debug debug mode}. This parameter supports the
     * following values (all the allowed values are case-insensitive):
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

    /**
     * Allows configuring arbitrary JVM arguments. Multiple arguments can be specified by delimiting them with a space
     * character.
     */
    @Parameter(defaultValue = "${jvm.args}")
    private String jvmArgs;

    @Parameter(defaultValue = "${quarkus.args}")
    private String argsString;

    @Parameter
    private Map<String, String> environmentVariables = Map.of();

    @Parameter
    private Map<String, String> systemProperties = Map.of();

    /**
     * When enabled, the {@code <environmentVariables>} and {@code <systemPropertyVariables>}
     * elements of the Maven Surefire plugin are copied to environment variables and system
     * properties defined by this plugin. Note that no other Surefire configuration is used
     * (notably {@code <systemProperties>}), only the 2 elements mentioned above.
     * <p>
     * This plugin's {@code <environmentVariables>} and {@code <systemProperties>} have
     * priority, so duplicate keys are not copied.
     * <p>
     * Since environment variables and system properties are global to the entire process,
     * this also affects dev mode (when executed as {@code quarkus:dev}). Because of that,
     * this copying action is disabled by default and requires opt-in.
     */
    @Parameter(defaultValue = "false")
    private boolean copySurefireVariables;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Parameter(defaultValue = "TRUE")
    private boolean deleteDevJar;

    @Component
    private MavenVersionEnforcer mavenVersionEnforcer;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Component
    private RepositorySystem repoSystem;

    @Component
    QuarkusWorkspaceProvider workspaceProvider;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true, required = true)
    private List<RemoteRepository> pluginRepos;

    /**
     * This value is intended to be set to true when we want to require C2 compilation instead of preventing it from
     * ever kicking in.
     * Setting this will likely have a small negative effect on startup time and should only be done when it absolutely
     * makes sense.
     */
    @Parameter(property = "forceC2")
    private Boolean forceC2;

    /**
     * Whether changes in the projects that appear to be dependencies of the project containing the application to be launched
     * should trigger hot-reload. By default, they do.
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
     * Additional compiler arguments
     */
    @Parameter
    private List<CompilerOptions> compilerOptions;

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
     * Whether to enforce the quarkus-maven-plugin build goal to be configured.
     * By default, a missing build goal is considered an inconsistency (although the build goal is not <i>required</i>
     * technically).
     * In this case a warning will be logged and the application will not be started.
     */
    @Parameter(defaultValue = "${quarkus.enforceBuildGoal}")
    private boolean enforceBuildGoal = true;

    @Parameter(property = "quarkus.warnIfBuildGoalMissing")
    boolean warnIfBuildGoalMissing = true;

    @Component
    private WorkspaceReader wsReader;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private ToolchainManager toolchainManager;

    private Map<ArtifactKey, Plugin> pluginMap;

    @Component
    protected QuarkusBootstrapProvider bootstrapProvider;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    MojoExecution mojoExecution;

    @Component
    BuildAnalyticsProvider analyticsProvider;

    /**
     * A comma-separated list of Maven plugin keys in {@code groupId:artifactId} format
     * (for example {@code org.codehaus.mojo:flatten-maven-plugin} and/or goal prefixes,
     * (for example {@code flatten}) that should be skipped when {@code quarkus:dev} identifies
     * Maven plugin goals that should be executed before the application is launched in dev mode.
     * <p>
     * Only the {@code flatten} Maven plugin is skipped by default.
     */
    @Parameter(defaultValue = "org.codehaus.mojo:flatten-maven-plugin")
    Set<String> skipPlugins;

    /**
     * Extension dev mode JVM option filter configuration.
     * <p>
     * Allows disabling all JVM options configured by extensions, for example
     *
     * <pre>{@code
     *     <extensionJvmOptions>
     *         <!-- disable JVM options from all the extensions -->
     *         <disableAll>true</disableAll>
     *     </extensionJvmOptions>*
     * }</pre>
     *
     * or specifying a {@code groupId:artifactId:classifier} artifact pattern
     * to disable options provided by the matching subset of extensions, for example
     *
     * <pre>{@code
     *     <extensionJvmOptions>
     *         <disableFor>
     *             <!-- disable JVM options from all the extensions with groupId org.acme -->
     *             <extension>org.acme</extension>
     *             <!-- disable JVM options configured by io.quarkiverse:quarkus-magic -->
     *             <extension>io.quarkiverse:quarkus-magic</extension>
     *         </disableFor>
     *     </extensionJvmOptions>
     * }</pre>
     */
    @Parameter
    ExtensionDevModeJvmOptionFilter extensionJvmOptions;

    /**
     * Selects given test(s) for continuous testing. This is an alternative to {@code quarkus.test.include-pattern}
     * and {@code quarkus.test.exclude-pattern}; if set, the {@code quarkus.test.[include|exclude]-pattern} configuration
     * is ignored.
     * <p>
     * The format of this configuration property is the same as the Maven Surefire {@code -Dtest=...}
     * <a href="https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#test">format</a>.
     * Specifically: it is a comma ({@code ,}) separated list of globs of class file paths and/or
     * method names. Each glob can potentially be prefixed with an exclamation mark ({@code !}), which makes
     * it an exclusion filter instead of an inclusion filter. Exclusions have higher priority than inclusions.
     * The class file path glob is separated from the method name glob by the hash sign ({@code #}) and multiple
     * method name globs may be present, separated by the plus sign ({@code +}).
     * <p>
     * For example:
     * <ul>
     * <li>{@code Basic*}: all classes starting with {@code Basic}</li>
     * <li>{@code ???Test}: all classes named with 3 arbitrary characters followed by {@code Test}</li>
     * <li>{@code !Unstable*}: all classes except classes starting with {@code Unstable}</li>
     * <li>{@code pkg/**}{@code /Ci*leTest}: all classes in the package {@code pkg} and subpackages, starting
     * with {@code Ci} and ending with {@code leTest}</li>
     * <li>{@code *Test#test*One+testTwo?????}: all classes ending with {@code Test}, and in them, only methods
     * starting with {@code test} and ending with {@code One}, or starting with {@code testTwo} and followed
     * by 5 arbitrary characters</li>
     * <li>{@code #fast*+slowTest}: all classes, and in them, only methods starting with {@code fast} or methods
     * named {@code slowTest}</li>
     * </ul>
     * Note that the syntax {@code %regex[...]} and {@code %ant[...]} is <em>NOT</em> supported.
     */
    @Parameter(property = "test")
    String test;

    /**
     * console attributes, used to restore the console state
     */
    private Attributes attributes;
    private int windowsAttributes;
    private boolean windowsAttributesSet;
    private Pty pty;
    private boolean windowsColorSupport;

    /**
     * Indicates for which launch mode the dependencies should be resolved.
     *
     * @return launch mode for which the dependencies should be resolved
     */
    protected LaunchMode getLaunchModeClasspath() {
        return LaunchMode.DEVELOPMENT;
    }

    @Override
    public void setLog(Log log) {
        super.setLog(log);
        MojoLogger.delegate = log;
    }

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {

        if (project.getPackaging().equals(ArtifactCoords.TYPE_POM)) {
            getLog().info("Type of the artifact is POM, skipping dev goal");
            return;
        }

        mavenVersionEnforcer.ensureMavenVersion(getLog(), session);

        initToolchain();

        // we always want to compile if needed, so if it is run from the parent it will compile dependent projects
        String bootstrapId = handleAutoCompile();

        if (enforceBuildGoal) {
            final PluginDescriptor pluginDescr = getPluginDescriptor();
            final Plugin pluginDef = getConfiguredPluginOrNull(pluginDescr.getGroupId(), pluginDescr.getArtifactId());
            if (!isGoalConfigured(pluginDef, "build")) {
                if (warnIfBuildGoalMissing) {
                    var currentGoal = getCurrentGoal();
                    getLog().warn(
                            "Skipping " + currentGoal + " as this is assumed to be a support library." +
                                    " To disable this warning set warnIfBuildGoalMissing parameter to false."
                                    + System.lineSeparator() +
                                    "To enable " + currentGoal +
                                    " for this module, make sure the quarkus-maven-plugin configuration includes the build goal"
                                    +
                                    " or disable the enforceBuildGoal flag (via plugin configuration or via" +
                                    " -Dquarkus.enforceBuildGoal=false).");
                }
                return;
            }
        }

        saveTerminalState();

        analyticsProvider.buildAnalyticsUserInput((String prompt) -> {
            System.out.print(prompt);
            try (Scanner scanner = new Scanner(new FilterInputStream(System.in) {
                @Override
                public void close() throws IOException {
                    //don't close System.in!
                }
            })) {
                return scanner.nextLine();
            } catch (Exception e) {
                getLog().debug("Failed to collect user input for analytics", e);
                return "";
            }
        });

        copySurefireVariables();

        try {
            DevModeRunner runner = new DevModeRunner(bootstrapId);
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
                    List<String> changedPoms = List.of();
                    for (Map.Entry<Path, Long> e : pomFiles.entrySet()) {
                        long t = Files.getLastModifiedTime(e.getKey()).toMillis();
                        if (t > e.getValue()) {
                            if (changedPoms.isEmpty()) {
                                // unless it's a git or some other command, there won't be many POMs modified in 100 milliseconds
                                changedPoms = new ArrayList<>(1);
                            }
                            changedPoms.add(e.getKey().toString());
                            pomFiles.put(e.getKey(), t);
                        }
                    }
                    if (!changedPoms.isEmpty()) {
                        logPomChanges(changedPoms);

                        // stop the runner before we build the new one as the debug port being free
                        // is tested when building the runner
                        runner.stop();

                        final DevModeRunner newRunner;
                        try {
                            bootstrapId = handleAutoCompile(changedPoms);
                            newRunner = new DevModeRunner(runner.commandLine.getDebugPort(), bootstrapId);
                        } catch (Exception e) {
                            getLog().info("Could not load changedPoms pom.xml file, changes not applied", e);
                            continue;
                        }
                        newRunner.run();
                        runner = newRunner;
                    }
                }
            }
        } catch (Exception e) {
            throw new MojoFailureException("Failed to run", e);
        }
    }

    private void logPomChanges(List<String> changedPoms) {
        final StringBuilder sb = new StringBuilder().append("Restarting dev mode following changes in ");
        sb.append(changedPoms.get(0));
        for (int i = 1; i < changedPoms.size(); ++i) {
            sb.append(", ").append(changedPoms.get(i));
        }
        getLog().info(sb.toString());
    }

    /**
     * if the process is forcibly killed then the terminal may be left in raw mode, which
     * messes everything up. This attempts to fix that by saving the state so it can be restored
     */
    private void saveTerminalState() {
        try {
            if (windowsAttributes > 0) {
                long hConsole = Kernel32.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
                if (hConsole != (long) Kernel32.INVALID_HANDLE_VALUE) {
                    int[] mode = new int[1];
                    windowsAttributes = Kernel32.GetConsoleMode(hConsole, mode) == 0 ? -1 : mode[0];
                    windowsAttributesSet = true;

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
            long hConsole = Kernel32.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
            if (hConsole != (long) Kernel32.INVALID_HANDLE_VALUE) {
                Kernel32.SetConsoleMode(hConsole, windowsAttributes);
            }
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

    private String handleAutoCompile() throws MojoExecutionException {
        return handleAutoCompile(List.of());
    }

    /**
     * Invokes Maven project goals that are meant to be executed before quarkus:dev,
     * unless they have already been executed.
     *
     * @param reloadPoms a list of POM files that should be reloaded from disk instead of read from the reactor
     * @return bootstrap id
     * @throws MojoExecutionException in case of an error
     */
    private String handleAutoCompile(List<String> reloadPoms) throws MojoExecutionException {
        List<String> goals = session.getGoals();
        // check for default goal(s) if none were specified explicitly,
        // see also org.apache.maven.lifecycle.internal.DefaultLifecycleTaskSegmentCalculator
        if (goals.isEmpty() && !StringUtils.isEmpty(project.getDefaultGoal())) {
            goals = List.of(StringUtils.split(project.getDefaultGoal()));
        }
        final String currentGoal = getCurrentGoal();

        int latestHandledPhaseIndex = -1;
        for (String goal : goals) {
            if (goal.endsWith(currentGoal)) {
                break;
            }
            if (goal.indexOf(':') >= 0 || IGNORED_PHASES.contains(goal)) {
                continue;
            }
            var i = PRE_DEV_MODE_PHASES.indexOf(goal);
            if (i < 0 || i == PRE_DEV_MODE_PHASES.size() - 1) {
                // all the necessary goals have already been executed
                return null;
            }
            if (i > latestHandledPhaseIndex) {
                latestHandledPhaseIndex = i;
            }
        }

        final String quarkusPluginId = getPluginDescriptor().getId();
        // configured plugin executions by lifecycle phases
        final Map<String, List<PluginExec>> phaseExecutions = new HashMap<>();
        // goals with prefixes on the command line
        final Map<String, Plugin> pluginPrefixes = new HashMap<>();
        String bootstrapId = BOOTSTRAP_ID;
        for (Plugin p : project.getBuildPlugins()) {
            if (p.getExecutions().isEmpty()) {
                continue;
            }
            if (skipPlugins.contains(p.getKey())) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Skipping " + p.getId() + " execution according to skipPlugins value");
                }
                continue;
            }
            for (PluginExecution e : p.getExecutions()) {
                if (e.getPhase() != null && !PRE_DEV_MODE_PHASES.contains(e.getPhase())) {
                    // skip executions with phases post quarkus:dev, such as install, deploy, site, etc
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Skipping " + e.getId() + " of " + p.getId());
                    }
                    continue;
                }
                String goalPrefix = null;
                if (!e.getGoals().isEmpty()) {
                    goalPrefix = getMojoDescriptor(p, e.getGoals().get(0)).getPluginDescriptor().getGoalPrefix();
                    if (skipPlugins.contains(goalPrefix)) {
                        if (getLog().isDebugEnabled()) {
                            getLog().debug("Skipping " + goalPrefix + " execution according to skipPlugins value");
                            continue;
                        }
                        continue;
                    }
                    pluginPrefixes.put(goalPrefix, p);
                    pluginPrefixes.put(p.getId(), p);
                }
                if (e.getPhase() != null) {
                    phaseExecutions.computeIfAbsent(e.getPhase(), k -> new ArrayList<>()).add(new PluginExec(p, goalPrefix, e));
                } else {
                    for (String goal : e.getGoals()) {
                        if (goal.equals(QUARKUS_GENERATE_CODE_GOAL) && p.getId().equals(quarkusPluginId)) {
                            var clone = e.clone();
                            clone.setGoals(List.of(QUARKUS_GENERATE_CODE_GOAL));
                            // this will schedule it before the compile phase goals
                            phaseExecutions.computeIfAbsent("compile", k -> new ArrayList<>())
                                    .add(0, new PluginExec(p, goalPrefix, clone));
                            bootstrapId = e.getId();
                        } else if (goal.equals(QUARKUS_GENERATE_CODE_TESTS_GOAL) && p.getId().equals(quarkusPluginId)) {
                            var clone = e.clone();
                            clone.setGoals(List.of(QUARKUS_GENERATE_CODE_TESTS_GOAL));
                            // this will schedule it before the test-compile phase goals
                            phaseExecutions.computeIfAbsent("test-compile", k -> new ArrayList<>())
                                    .add(0, new PluginExec(p, goalPrefix, clone));
                        } else {
                            var mojoDescr = getMojoDescriptor(p, goal);
                            if (mojoDescr.getPhase() != null) {
                                phaseExecutions.computeIfAbsent(mojoDescr.getPhase(), k -> new ArrayList<>())
                                        .add(new PluginExec(p, goalPrefix, e));
                            } else {
                                getLog().warn("Failed to determine the lifecycle phase for " + p.getId() + ":" + goal);
                            }
                        }
                    }
                }
            }
        }

        // Map<pluginId, List<goals>>
        final Map<String, List<String>> executedPluginGoals = new HashMap<>();
        for (String goal : goals) {
            if (goal.endsWith(currentGoal)) {
                break;
            }
            var colon = goal.lastIndexOf(':');
            if (colon >= 0) {
                var plugin = pluginPrefixes.get(goal.substring(0, colon));
                if (plugin == null) {
                    getLog().warn("Failed to locate plugin for " + goal);
                } else {
                    executedPluginGoals.computeIfAbsent(plugin.getId(), k -> new ArrayList<>()).add(goal.substring(colon + 1));
                }
            }
        }

        Map<String, String> quarkusGoalParams = null;
        for (int phaseIndex = latestHandledPhaseIndex + 1; phaseIndex < PRE_DEV_MODE_PHASES.size(); ++phaseIndex) {
            var executions = phaseExecutions.get(PRE_DEV_MODE_PHASES.get(phaseIndex));
            if (executions == null) {
                continue;
            }
            for (PluginExec pe : executions) {
                var executedGoals = executedPluginGoals.getOrDefault(pe.plugin.getId(), List.of());
                for (String goal : pe.execution.getGoals()) {
                    if (!executedGoals.contains(goal)) {
                        if (quarkusGoalParams == null) {
                            quarkusGoalParams = getQuarkusGoalParams(bootstrapId, reloadPoms);
                        }
                        try {
                            executeGoal(pe, goal,
                                    pe.getPluginId().equals(quarkusPluginId) ? quarkusGoalParams : Map.of());
                        } catch (Throwable t) {
                            if (goal.equals("testCompile")) {
                                getLog().error(
                                        "Test compile failed, you will need to fix your tests before you can use continuous testing",
                                        t);
                            } else {
                                throw t;
                            }
                        }
                    }
                }
            }
        }
        return bootstrapId;
    }

    /**
     * Returns a map of parameters for the Quarkus plugin goals to be invoked.
     *
     * @param bootstrapId bootstrap id
     * @param reloadPoms POM files to be reloaded from disk instead of taken from the reactor
     * @return map of parameters for the Quarkus plugin goals
     */
    private Map<String, String> getQuarkusGoalParams(String bootstrapId, List<String> reloadPoms) {
        final Map<String, String> result = new HashMap<>(4);
        result.put(QuarkusBootstrapMojo.MODE_PARAM, getLaunchModeClasspath().name());
        result.put(QuarkusBootstrapMojo.CLOSE_BOOTSTRAPPED_APP_PARAM, "false");
        result.put(QuarkusBootstrapMojo.BOOTSTRAP_ID_PARAM, bootstrapId);
        if (reloadPoms != null && !reloadPoms.isEmpty()) {
            String reloadPomsStr;
            if (reloadPoms.size() == 1) {
                reloadPomsStr = reloadPoms.get(0);
            } else {
                final StringBuilder sb = new StringBuilder();
                sb.append(reloadPoms.get(0));
                for (int i = 1; i < reloadPoms.size(); ++i) {
                    sb.append(",").append(reloadPoms.get(i));
                }
                reloadPomsStr = sb.toString();
            }
            result.put(QuarkusBootstrapMojo.RELOAD_POMS_PARAM, reloadPomsStr);
        }
        return result;
    }

    private String getCurrentGoal() {
        return mojoExecution.getMojoDescriptor().getPluginDescriptor().getGoalPrefix() + ":"
                + mojoExecution.getGoal();
    }

    private PluginDescriptor getPluginDescriptor() {
        return mojoExecution.getMojoDescriptor().getPluginDescriptor();
    }

    private void initToolchain() throws MojoExecutionException {
        final Plugin plugin = getConfiguredPluginOrNull(ORG_APACHE_MAVEN_PLUGINS, MAVEN_TOOLCHAINS_PLUGIN);
        if (!isGoalConfigured(plugin, "toolchain")) {
            return;
        }
        executeGoal(new PluginExec(plugin, null, null), "toolchain", Map.of());
    }

    private void executeGoal(PluginExec pluginExec, String goal, Map<String, String> params)
            throws MojoExecutionException {
        var msg = new StringBuilder();
        msg.append("Invoking ")
                .append(pluginExec.getPrefix()).append(":")
                .append(pluginExec.plugin.getVersion()).append(":")
                .append(goal);
        if (pluginExec.getExecutionId() != null) {
            msg.append(" (").append(pluginExec.getExecutionId()).append(")");
        }
        msg.append(" @ ").append(project.getArtifactId());
        getLog().info(msg.toString());
        executeMojo(
                plugin(
                        groupId(pluginExec.plugin.getGroupId()),
                        artifactId(pluginExec.plugin.getArtifactId()),
                        version(pluginExec.plugin.getVersion()),
                        pluginExec.plugin.getDependencies()),
                goal(goal),
                getPluginConfig(pluginExec.plugin, pluginExec.getExecutionId(), goal, params),
                executionEnvironment(
                        project,
                        session,
                        pluginManager));
    }

    private List<String> readAnnotationProcessors(Xpp3Dom pluginConfig) {
        if (pluginConfig == null) {
            return List.of();
        }
        Xpp3Dom annotationProcessors = pluginConfig.getChild("annotationProcessors");
        if (annotationProcessors == null) {
            return List.of();
        }
        Xpp3Dom[] processors = annotationProcessors.getChildren("annotationProcessor");
        if (processors.length == 0) {
            return List.of();
        }
        List<String> ret = new ArrayList<>(processors.length);
        for (Xpp3Dom processor : processors) {
            ret.add(processor.getValue());
        }
        return ret;
    }

    private Set<File> readAnnotationProcessorPaths(Xpp3Dom pluginConfig) throws MojoExecutionException {
        if (pluginConfig == null) {
            return Set.of();
        }
        Xpp3Dom annotationProcessorPaths = pluginConfig.getChild("annotationProcessorPaths");
        if (annotationProcessorPaths == null) {
            return Set.of();
        }
        var versionConstraints = getAnnotationProcessorPathsDepMgmt(pluginConfig);
        Xpp3Dom[] paths = annotationProcessorPaths.getChildren("path");
        Set<File> elements = new LinkedHashSet<>();
        try {
            List<org.eclipse.aether.graph.Dependency> dependencies = convertToDependencies(paths);
            CollectRequest collectRequest = new CollectRequest(dependencies, versionConstraints,
                    project.getRemoteProjectRepositories());
            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setCollectRequest(collectRequest);
            DependencyResult dependencyResult = repoSystem.resolveDependencies(session.getRepositorySession(),
                    dependencyRequest);

            for (ArtifactResult resolved : dependencyResult.getArtifactResults()) {
                elements.add(resolved.getArtifact().getFile());
            }
            return elements;
        } catch (Exception e) {
            throw new MojoExecutionException(
                    "Resolution of annotationProcessorPath dependencies failed: " + e.getLocalizedMessage(), e);
        }
    }

    private List<org.eclipse.aether.graph.Dependency> getAnnotationProcessorPathsDepMgmt(Xpp3Dom pluginConfig) {
        final Xpp3Dom useDepMgmt = pluginConfig.getChild("annotationProcessorPathsUseDepMgmt");
        if (useDepMgmt == null || !Boolean.parseBoolean(useDepMgmt.getValue())) {
            return List.of();
        }
        var dm = project.getDependencyManagement();
        if (dm == null) {
            return List.of();
        }
        return getProjectAetherDependencyManagement();
    }

    private List<org.eclipse.aether.graph.Dependency> convertToDependencies(Xpp3Dom[] paths) throws MojoExecutionException {
        List<org.eclipse.aether.graph.Dependency> dependencies = new ArrayList<>();
        for (Xpp3Dom path : paths) {
            String type = getValue(path, "type", "jar");
            ArtifactHandler handler = artifactHandlerManager.getArtifactHandler(type);
            // WATCH OUT: this constructor turns any null values into empty strings
            org.eclipse.aether.artifact.Artifact artifact = new DefaultArtifact(
                    getValue(path, "groupId", null),
                    getValue(path, "artifactId", null),
                    getValue(path, "classifier", null),
                    handler.getExtension(),
                    getValue(path, "version", null));
            if (toNullIfEmpty(artifact.getVersion()) == null) {
                artifact = artifact.setVersion(getAnnotationProcessorPathVersion(artifact));
            }
            Set<org.eclipse.aether.graph.Exclusion> exclusions = convertToAetherExclusions(path.getChild("exclusions"));
            dependencies.add(new org.eclipse.aether.graph.Dependency(artifact, JavaScopes.RUNTIME, false, exclusions));
        }
        return dependencies;
    }

    private String getAnnotationProcessorPathVersion(org.eclipse.aether.artifact.Artifact annotationProcessorPath)
            throws MojoExecutionException {
        List<Dependency> managedDependencies = getProjectManagedDependencies();
        return findManagedVersion(annotationProcessorPath, managedDependencies)
                .orElseThrow(() -> new MojoExecutionException(String.format(
                        "Cannot find version for annotation processor path '%s'. The version needs to be either"
                                + " provided directly in the plugin configuration or via dependency management.",
                        annotationProcessorPath)));
    }

    private Optional<String> findManagedVersion(
            org.eclipse.aether.artifact.Artifact artifact, List<Dependency> managedDependencies) {
        // here, Dependency uses null, while artifact uses empty strings
        return managedDependencies.stream()
                .filter(dep -> Objects.equals(dep.getGroupId(), artifact.getGroupId())
                        && Objects.equals(dep.getArtifactId(), artifact.getArtifactId())
                        && Objects.equals(dep.getClassifier(), toNullIfEmpty(artifact.getClassifier()))
                        && Objects.equals(dep.getType(), toNullIfEmpty(artifact.getExtension())))
                .findAny()
                .map(org.apache.maven.model.Dependency::getVersion);
    }

    private String toNullIfEmpty(String value) {
        if (value != null && value.isBlank())
            return null;
        return value;
    }

    private List<Dependency> getProjectManagedDependencies() {
        DependencyManagement dependencyManagement = project.getDependencyManagement();
        if (dependencyManagement == null || dependencyManagement.getDependencies() == null) {
            return List.of();
        }
        return dependencyManagement.getDependencies();
    }

    private String getValue(Xpp3Dom path, String element, String defaultValue) {
        Xpp3Dom child = path.getChild(element);
        // don't bother filtering empty strings or null values, DefaultArtifact will turn nulls into empty strings
        if (child == null) {
            return defaultValue;
        }
        return child.getValue();
    }

    private Set<org.eclipse.aether.graph.Exclusion> convertToAetherExclusions(Xpp3Dom exclusions) {
        if (exclusions == null) {
            return Set.of();
        }
        Set<Exclusion> aetherExclusions = new HashSet<>();
        for (Xpp3Dom exclusion : exclusions.getChildren("exclusion")) {
            Exclusion aetherExclusion = new Exclusion(
                    getValue(exclusion, "groupId", null),
                    getValue(exclusion, "artifactId", null),
                    getValue(exclusion, "classifier", null),
                    getValue(exclusion, "extension", "jar"));
            aetherExclusions.add(aetherExclusion);
        }
        return aetherExclusions;
    }

    private boolean isGoalConfigured(Plugin plugin, String goal) {
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

    private Xpp3Dom getPluginConfig(Plugin plugin, String executionId, String goal, Map<String, String> params)
            throws MojoExecutionException {
        Xpp3Dom mergedConfig = null;
        if (!plugin.getExecutions().isEmpty()) {
            for (PluginExecution exec : plugin.getExecutions()) {
                if (exec.getConfiguration() != null && exec.getGoals().contains(goal)
                        && matchesExecution(executionId, exec.getId())) {
                    mergedConfig = mergedConfig == null ? (Xpp3Dom) exec.getConfiguration()
                            : Xpp3Dom.mergeXpp3Dom(mergedConfig, (Xpp3Dom) exec.getConfiguration(), true);
                }
            }
        }

        if (plugin.getConfiguration() != null) {
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

    /**
     * Check if the <code>currentExecutionId</code> matches the provided <code>executionId</code>.
     * <p>
     * This method will return <code>true</code> if
     * <ul>
     * <li>current execution id is undefined</li>
     * <li>execution id is undefined</li>
     * <li>both equals (ignoring case)</li>
     * </ul>
     *
     * @param currentExecutionId current execution id (if defined)
     * @param executionId execution id to test matching (if defined)
     * @return <code>true</code> if executions ids do match.
     */
    private static boolean matchesExecution(String currentExecutionId, String executionId) {
        if (currentExecutionId == null) {
            return true;
        }
        return executionId == null || currentExecutionId.equalsIgnoreCase(executionId);
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
                pluginMap.put(ArtifactKey.ga(p.getGroupId(), p.getArtifactId()), p);
            }
        }
        return pluginMap.get(ArtifactKey.ga(groupId, artifactId));
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

    private void addProject(DevModeCommandLineBuilder builder, ResolvedDependency module, boolean root) throws Exception {
        if (!module.isJar()) {
            return;
        }

        String projectDirectory;
        Set<Path> sourcePaths;
        String classesPath = null;
        String generatedSourcesPath = null;
        Set<Path> resourcePaths;
        Set<Path> testSourcePaths;
        String testClassesPath = null;
        Set<Path> testResourcePaths;
        List<Profile> activeProfiles = List.of();

        MavenProject mavenProject = null;
        if (module.getClassifier().isEmpty()) {
            for (MavenProject p : session.getAllProjects()) {
                if (module.getArtifactId().equals(p.getArtifactId())
                        && module.getGroupId().equals(p.getGroupId())
                        && module.getVersion().equals(p.getVersion())) {
                    mavenProject = p;
                    break;
                }
            }
        }
        final ArtifactSources sources = module.getSources();
        if (mavenProject == null) {
            if (sources == null) {
                getLog().debug("Local dependency " + module.toCompactCoords() + " does not appear to have any sources");
                return;
            }
            projectDirectory = module.getWorkspaceModule().getModuleDir().getAbsolutePath();
            sourcePaths = new LinkedHashSet<>();
            for (SourceDir src : sources.getSourceDirs()) {
                for (Path p : src.getSourceTree().getRoots()) {
                    sourcePaths.add(p.toAbsolutePath());
                }
            }
            testSourcePaths = new LinkedHashSet<>();
            ArtifactSources testSources = module.getWorkspaceModule().getTestSources();
            if (testSources != null) {
                for (SourceDir src : testSources.getSourceDirs()) {
                    for (Path p : src.getSourceTree().getRoots()) {
                        testSourcePaths.add(p.toAbsolutePath());
                    }
                }
            }
        } else {
            projectDirectory = mavenProject.getBasedir().getPath();
            sourcePaths = mavenProject.getCompileSourceRoots().stream()
                    .map(Path::of)
                    .map(Path::toAbsolutePath)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            testSourcePaths = mavenProject.getTestCompileSourceRoots().stream()
                    .map(Path::of)
                    .map(Path::toAbsolutePath)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            activeProfiles = mavenProject.getActiveProfiles();
        }

        final Path sourceParent;
        if (sourcePaths.isEmpty()) {
            if (sources == null || sources.getResourceDirs() == null) {
                throw new MojoExecutionException(
                        "Local dependency " + module.toCompactCoords() + " does not appear to have any sources");
            }
            sourceParent = sources.getResourceDirs().iterator().next().getDir().toAbsolutePath().getParent();
        } else {
            sourceParent = sourcePaths.iterator().next().toAbsolutePath().getParent();
        }

        Path classesDir = null;
        resourcePaths = new LinkedHashSet<>();
        if (sources != null) {
            SourceDir firstSourceDir = sources.getSourceDirs().iterator().next();
            classesDir = firstSourceDir.getOutputDir().toAbsolutePath();
            if (firstSourceDir.getAptSourcesDir() != null) {
                generatedSourcesPath = firstSourceDir.getAptSourcesDir().toAbsolutePath().toString();
            }
            if (Files.isDirectory(classesDir)) {
                classesPath = classesDir.toString();
            }
            for (SourceDir src : sources.getResourceDirs()) {
                for (Path p : src.getSourceTree().getRoots()) {
                    resourcePaths.add(p.toAbsolutePath());
                }
            }
        }
        if (module.getWorkspaceModule().hasTestSources()) {
            Path testClassesDir = module.getWorkspaceModule().getTestSources().getSourceDirs().iterator().next().getOutputDir()
                    .toAbsolutePath();
            testClassesPath = testClassesDir.toString();
        }

        testResourcePaths = new LinkedHashSet<>();
        ArtifactSources testSources = module.getWorkspaceModule().getTestSources();
        if (testSources != null) {
            for (SourceDir src : testSources.getResourceDirs()) {
                for (Path p : src.getSourceTree().getRoots()) {
                    testResourcePaths.add(p.toAbsolutePath());
                }
            }
        }

        // Add the resources and test resources from the profiles
        for (Profile profile : activeProfiles) {
            final BuildBase build = profile.getBuild();
            if (build != null) {
                resourcePaths.addAll(
                        build.getResources().stream()
                                .map(Resource::getDirectory)
                                .map(Path::of)
                                .map(Path::toAbsolutePath)
                                .collect(Collectors.toList()));
                testResourcePaths.addAll(
                        build.getTestResources().stream()
                                .map(Resource::getDirectory)
                                .map(Path::of)
                                .map(Path::toAbsolutePath)
                                .collect(Collectors.toList()));
            }
        }

        if (classesPath == null && (!sourcePaths.isEmpty() || !resourcePaths.isEmpty())) {
            throw new MojoExecutionException("Hot reloadable dependency " + module.getWorkspaceModule().getId()
                    + " has not been compiled yet (the classes directory " + (classesDir == null ? "" : classesDir)
                    + " does not exist)");
        }

        Path targetDir = Path.of(project.getBuild().getDirectory());

        // In some cases, we may have a generatedSourcesPath that has not been created, or that has been deleted
        // after Maven created it. Javac will not be happy about it, and we will mimic Maven and auto-create it
        // when this happens, to avoid crashing the compiler
        if (generatedSourcesPath != null && Files.notExists(Path.of(generatedSourcesPath))) {
            Files.createDirectories(Path.of(generatedSourcesPath));
        }

        DevModeContext.ModuleInfo moduleInfo = new DevModeContext.ModuleInfo.Builder()
                .setArtifactKey(module.getKey())
                .setProjectDirectory(projectDirectory)
                .setSourcePaths(PathList.from(sourcePaths))
                .setClassesPath(classesPath)
                .setGeneratedSourcesPath(generatedSourcesPath)
                .setResourcesOutputPath(classesPath)
                .setResourcePaths(PathList.from(resourcePaths))
                .setSourceParents(PathList.of(sourceParent.toAbsolutePath()))
                .setPreBuildOutputDir(targetDir.resolve("generated-sources").toAbsolutePath().toString())
                .setTargetDir(targetDir.toAbsolutePath().toString())
                .setTestSourcePaths(PathList.from(testSourcePaths))
                .setTestClassesPath(testClassesPath)
                .setTestResourcesOutputPath(testClassesPath)
                .setTestResourcePaths(PathList.from(testResourcePaths))
                .build();

        if (root) {
            builder.mainModule(moduleInfo);
        } else {
            builder.dependency(moduleInfo);
        }
    }

    private class DevModeRunner {

        final DevModeCommandLine commandLine;
        private Process process;

        private DevModeRunner(String bootstrapId) throws Exception {
            commandLine = newLauncher(null, bootstrapId);
        }

        private DevModeRunner(String actualDebugPort, String bootstrapId) throws Exception {
            commandLine = newLauncher(actualDebugPort, bootstrapId);
        }

        Collection<Path> pomFiles() {
            return commandLine.getWatchedBuildFiles();
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
                getLog().debug("Launching JVM with command line: " + String.join(" ", commandLine.getArguments()));
            }
            final ProcessBuilder processBuilder = new ProcessBuilder(commandLine.getArguments())
                    .redirectErrorStream(true)
                    .inheritIO()
                    .directory(workingDir == null ? project.getBasedir() : workingDir);
            if (!environmentVariables.isEmpty()) {
                processBuilder.environment().putAll(environmentVariables);
            }
            process = processBuilder.start();

            //https://github.com/quarkusio/quarkus/issues/232
            Runtime.getRuntime().addShutdownHook(new Thread(this::safeStop, "Development Mode Shutdown Hook"));
        }

        private void safeStop() {
            try {
                stop();
            } catch (InterruptedException e) {
                getLog().warn("Unable to properly wait for dev-mode end", e);
            }
        }

        void stop() throws InterruptedException {
            process.destroy();
            process.waitFor();
        }
    }

    private DevModeCommandLine newLauncher(String actualDebugPort, String bootstrapId) throws Exception {
        String java = null;
        // See if a toolchain is configured
        if (toolchainManager != null) {
            Toolchain toolchain = toolchainManager.getToolchainFromBuildContext("jdk", session);
            if (toolchain != null) {
                java = toolchain.findTool("java");
                getLog().info("JVM from toolchain: " + java);
            }
        }

        final DevModeCommandLineBuilder builder = DevModeCommandLine.builder(java)
                .forceC2(forceC2)
                .buildDir(buildDir)
                .outputDir(outputDirectory)
                .suspend(suspend)
                .debug(debug)
                .debugHost(debugHost)
                .debugPort(actualDebugPort)
                .deleteDevJar(deleteDevJar);

        setJvmArgs(builder);
        if (windowsColorSupport) {
            builder.jvmArgs("-Dio.quarkus.force-color-support=true");
        }
        if (test != null) {
            builder.jvmArgs("-Dquarkus-internal.test.specific-selection=maven:" + test);
        }

        if (openJavaLang) {
            builder.addOpens("java.base/java.lang=ALL-UNNAMED");
        }

        if (modules != null && !modules.isEmpty()) {
            builder.addModules(this.modules);
        }

        builder.projectDir(project.getFile().getParentFile());

        Properties projectProperties = project.getProperties();
        Map<String, String> effectiveProperties = new HashMap<>();
        for (String name : projectProperties.stringPropertyNames()) {
            if (name.startsWith("quarkus.")) {
                effectiveProperties.put(name, projectProperties.getProperty(name));
            }
        }

        // Add other properties that may be required for expansion
        List<String> effectivePropertyValues = new ArrayList<>(effectiveProperties.values());
        for (String value : effectivePropertyValues) {
            for (String reference : Expression.compile(value, LENIENT_SYNTAX, NO_TRIM).getReferencedStrings()) {
                String referenceValue = session.getUserProperties().getProperty(reference);
                if (referenceValue != null) {
                    effectiveProperties.put(reference, referenceValue);
                    continue;
                }

                referenceValue = projectProperties.getProperty(reference);
                if (referenceValue != null) {
                    effectiveProperties.put(reference, referenceValue);
                }
            }
        }
        builder.buildSystemProperties(effectiveProperties);

        builder.applicationName(project.getArtifactId());
        builder.applicationVersion(project.getVersion());

        builder.sourceEncoding(getSourceEncoding());

        if (compilerOptions != null) {
            for (CompilerOptions compilerOption : compilerOptions) {
                builder.compilerOptions(compilerOption.getName(), compilerOption.getArgs());
            }
        }

        // Set compilation flags.  Try the explicitly given configuration first.  Otherwise,
        // refer to the configuration of the Maven Compiler Plugin.
        final Optional<Xpp3Dom> compilerPluginConfiguration = findCompilerPluginConfiguration();
        if (compilerArgs != null) {
            builder.compilerOptions("java", compilerArgs);
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
                builder.compilerOptions("java", compilerPluginArgs);
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
        setAnnotationProcessorFlags(builder);

        // path to the serialized application model
        final Path appModelLocation = resolveSerializedModelLocation();

        ApplicationModel appModel = bootstrapProvider.getResolvedApplicationModel(
                QuarkusBootstrapProvider.getProjectId(project), getLaunchModeClasspath(), bootstrapId);
        if (appModel != null) {
            bootstrapProvider.close();
        } else {
            Path rootProjectDir = null;
            String topLevelBaseDirStr = systemProperties.get(BootstrapMavenContext.MAVEN_TOP_LEVEL_PROJECT_BASEDIR);
            if (topLevelBaseDirStr != null) {
                final Path tmp = Path.of(topLevelBaseDirStr);
                if (!Files.exists(tmp)) {
                    throw new BootstrapMavenException("Top-level project base directory " + topLevelBaseDirStr
                            + " specified with system property " + BootstrapMavenContext.MAVEN_TOP_LEVEL_PROJECT_BASEDIR
                            + " does not exist");
                }
                rootProjectDir = tmp;
            }

            final BootstrapMavenContextConfig<?> mvnConfig = BootstrapMavenContext.config()
                    .setUserSettings(session.getRequest().getUserSettingsFile())
                    .setRemoteRepositories(repos)
                    .setWorkspaceDiscovery(true)
                    .setPreferPomsFromWorkspace(true)
                    // it's important to set the base directory instead of the POM
                    // which maybe manipulated by a plugin and stored outside the base directory
                    .setCurrentProject(project.getBasedir().toString())
                    .setEffectiveModelBuilder(BootstrapMavenContextConfig.getEffectiveModelBuilderProperty(projectProperties))
                    .setRootProjectDir(rootProjectDir);

            // There are a couple of reasons we don't want to use the original Maven session:
            // 1) a reload could be triggered by a change in a pom.xml, in which case the Maven session might not be in sync anymore with the effective POM;
            // 2) in case there is a local module that has a snapshot version, which is also available in a remote snapshot repository,
            // the Maven resolver will be checking for newer snapshots in the remote repository and might end up resolving the artifact from there.
            final BootstrapMavenContext mvnCtx = workspaceProvider.createMavenContext(mvnConfig);
            appModel = new BootstrapAppModelResolver(new MavenArtifactResolver(mvnCtx))
                    .setDevMode(getLaunchModeClasspath().isDevOrTest())
                    .setTest(LaunchMode.TEST.equals(getLaunchModeClasspath()))
                    .setCollectReloadableDependencies(!noDeps)
                    .setLegacyModelResolver(BootstrapAppModelResolver.isLegacyModelResolver(project.getProperties()))
                    .resolveModel(mvnCtx.getCurrentProject().getAppArtifact());
        }

        builder.extensionDevModeConfig(appModel.getExtensionDevModeConfig())
                .extensionDevModeJvmOptionFilter(extensionJvmOptions);

        // serialize the app model to avoid re-resolving it in the dev process
        BootstrapUtils.serializeAppModel(appModel, appModelLocation);
        builder.jvmArgs("-D" + BootstrapConstants.SERIALIZED_APP_MODEL + "=" + appModelLocation);

        if (noDeps) {
            addProject(builder, appModel.getAppArtifact(), true);
            appModel.getApplicationModule().getBuildFiles().forEach(builder::watchedBuildFile);
            builder.localArtifact(
                    ArtifactKey.of(project.getGroupId(), project.getArtifactId(), null, ArtifactCoords.TYPE_JAR));
        } else {
            for (ResolvedDependency project : DependenciesFilter.getReloadableModules(appModel)) {
                addProject(builder, project, project == appModel.getAppArtifact());
                project.getWorkspaceModule().getBuildFiles().forEach(builder::watchedBuildFile);
                builder.localArtifact(project.getKey());
            }
        }

        addQuarkusDevModeDeps(builder, appModel);
        //look for an application.properties
        Set<Path> resourceDirs = new HashSet<>();
        for (Resource resource : project.getResources()) {
            String dir = resource.getDirectory();
            Path path = Paths.get(dir);
            resourceDirs.add(path);
        }

        //in most cases these are not used, however they need to be present for some
        //parent-first cases such as logging
        //first we go through and get all the parent first artifacts
        final Collection<ArtifactKey> configuredParentFirst = ConfiguredClassLoading.builder()
                .setApplicationModel(appModel)
                .setApplicationRoot(PathsCollection.from(resourceDirs))
                .setMode(QuarkusBootstrap.Mode.DEV)
                .build().getParentFirstArtifacts();

        for (Artifact appDep : project.getArtifacts()) {
            // only add the artifact if it's present in the dev mode context
            // we need this to avoid having jars on the classpath multiple times
            final ArtifactKey key = ArtifactKey.of(appDep.getGroupId(), appDep.getArtifactId(),
                    appDep.getClassifier(), appDep.getArtifactHandler().getExtension());
            if (!builder.isLocal(key) && configuredParentFirst.contains(key)) {
                builder.classpathEntry(key, appDep.getFile());
            }
        }

        builder.baseName(project.getBuild().getFinalName());

        modifyDevModeContext(builder);

        if (argsString != null) {
            builder.applicationArgs(argsString);
        }
        analyticsProvider.sendAnalytics(DEV_MODE, appModel, emptyMap(), buildDir);
        return builder.build();
    }

    private void debug(String msg, Object... args) {
        if (getLog().isDebugEnabled()) {
            getLog().debug(String.format(msg, args));
        }
    }

    private void setJvmArgs(DevModeCommandLineBuilder builder) throws Exception {
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
            final String[] arr = CommandLineUtils.translateCommandline(jvmArgs);
            final List<String> list = new ArrayList<>(arr.length);
            for (var s : arr) {
                list.add(s.trim());
            }
            builder.jvmArgs(list);
        }
    }

    private void copySurefireVariables() {
        if (!copySurefireVariables) {
            return;
        }

        Plugin surefireMavenPlugin = getConfiguredPluginOrNull(ORG_APACHE_MAVEN_PLUGINS, MAVEN_SUREFIRE_PLUGIN);
        if (surefireMavenPlugin == null) {
            return;
        }

        ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
        Xpp3Dom config = (Xpp3Dom) surefireMavenPlugin.getConfiguration();
        if (config != null) {
            // we copy the maps because they can be unmodifiable
            environmentVariables = new HashMap<>(environmentVariables);
            copyConfiguration(config.getChild("environmentVariables"), environmentVariables, evaluator);
            systemProperties = new HashMap<>(systemProperties);
            copyConfiguration(config.getChild("systemPropertyVariables"), systemProperties, evaluator);
        }
    }

    private void copyConfiguration(Xpp3Dom config, Map<String, String> targetMap, ExpressionEvaluator evaluator) {
        if (config == null) {
            return;
        }
        for (Xpp3Dom child : config.getChildren()) {
            targetMap.computeIfAbsent(child.getName(), ignored -> {
                try {
                    Object value = evaluator.evaluate(child.getValue());
                    return value == null ? null : value.toString();
                } catch (ExpressionEvaluationException e) {
                    throw new RuntimeException(e);
                }
            });
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

    private void addQuarkusDevModeDeps(DevModeCommandLineBuilder builder, ApplicationModel appModel)
            throws MojoExecutionException, DependencyResolutionException {

        ResolvedDependency coreDeployment = null;
        for (ResolvedDependency d : appModel.getDependencies()) {
            if (d.isDeploymentCp() && d.getArtifactId().equals("quarkus-core-deployment")
                    && d.getGroupId().equals(IO_QUARKUS)) {
                coreDeployment = d;
                break;
            }
        }
        if (coreDeployment == null) {
            throw new MojoExecutionException(
                    "Failed to locate io.quarkus:quarkus-core-deployment on the application build classpath");
        }

        final String pomPropsPath = "META-INF/maven/io.quarkus/quarkus-bootstrap-maven-resolver/pom.properties";
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

        final DefaultArtifact devModeJar = new DefaultArtifact(devModeGroupId, devModeArtifactId, ArtifactCoords.TYPE_JAR,
                devModeVersion);
        final DependencyResult cpRes = repoSystem.resolveDependencies(repoSession,
                new DependencyRequest()
                        .setCollectRequest(
                                new CollectRequest()
                                        // it doesn't matter what the root artifact is, it's an alias
                                        .setRootArtifact(new DefaultArtifact(IO_QUARKUS, "quarkus-devmode-alias",
                                                ArtifactCoords.TYPE_JAR, "1.0"))
                                        .setManagedDependencies(getProjectAetherDependencyManagement())
                                        .setDependencies(List.of(
                                                new org.eclipse.aether.graph.Dependency(devModeJar, JavaScopes.RUNTIME),
                                                new org.eclipse.aether.graph.Dependency(new DefaultArtifact(
                                                        coreDeployment.getGroupId(), coreDeployment.getArtifactId(),
                                                        coreDeployment.getClassifier(), coreDeployment.getType(),
                                                        coreDeployment.getVersion()), JavaScopes.RUNTIME)))
                                        .setRepositories(repos)));

        for (ArtifactResult appDep : cpRes.getArtifactResults()) {
            //we only use the launcher for launching from the IDE, we need to exclude it
            final org.eclipse.aether.artifact.Artifact a = appDep.getArtifact();
            if (!(a.getArtifactId().equals("quarkus-ide-launcher")
                    && a.getGroupId().equals(IO_QUARKUS))) {
                if (a.getArtifactId().equals("quarkus-class-change-agent")
                        && a.getGroupId().equals(IO_QUARKUS)) {
                    builder.jvmArgs("-javaagent:" + a.getFile().getAbsolutePath());
                } else {
                    builder.classpathEntry(
                            ArtifactKey.of(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getExtension()),
                            a.getFile());
                }
            }
        }
    }

    private List<org.eclipse.aether.graph.Dependency> getProjectAetherDependencyManagement() {
        final List<org.eclipse.aether.graph.Dependency> managed = new ArrayList<>(
                project.getDependencyManagement().getDependencies().size());
        project.getDependencyManagement().getDependencies().forEach(d -> {
            final List<Exclusion> exclusions;
            if (!d.getExclusions().isEmpty()) {
                exclusions = new ArrayList<>(d.getExclusions().size());
                d.getExclusions().forEach(e -> exclusions.add(new Exclusion(e.getGroupId(), e.getArtifactId(), "*", "*")));
            } else {
                exclusions = List.of();
            }
            managed.add(new org.eclipse.aether.graph.Dependency(
                    new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion()),
                    d.getScope(), d.isOptional(), exclusions));
        });
        return managed;
    }

    private void setKotlinSpecificFlags(DevModeCommandLineBuilder builder) {
        Plugin kotlinMavenPlugin = getConfiguredPluginOrNull(ORG_JETBRAINS_KOTLIN, KOTLIN_MAVEN_PLUGIN);
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

    private void setAnnotationProcessorFlags(DevModeCommandLineBuilder builder) {
        Plugin compilerMavenPlugin = getConfiguredPluginOrNull(ORG_APACHE_MAVEN_PLUGINS, MAVEN_COMPILER_PLUGIN);
        if (compilerMavenPlugin == null) {
            return;
        }

        getLog().debug("Maven compiler plugin found, looking for annotation processors");

        List<String> options = new ArrayList<>();
        Xpp3Dom compilerPluginConfiguration = (Xpp3Dom) compilerMavenPlugin.getConfiguration();
        try {
            Set<File> processorPaths = this.readAnnotationProcessorPaths(compilerPluginConfiguration);
            getLog().debug("Found processor paths: " + processorPaths);
            if (!processorPaths.isEmpty()) {
                builder.annotationProcessorPaths(processorPaths);
            }
        } catch (MojoExecutionException e) {
            throw new RuntimeException(e);
        }
        List<String> processors = this.readAnnotationProcessors(compilerPluginConfiguration);
        getLog().debug("Found processors: " + processors);
        if (!processors.isEmpty()) {
            builder.annotationProcessors(processors);
        }
        builder.compilerPluginOptions(options);
    }

    protected void modifyDevModeContext(DevModeCommandLineBuilder builder) {

    }

    private Optional<Xpp3Dom> findCompilerPluginConfiguration() {
        Plugin plugin = getConfiguredPluginOrNull(ORG_APACHE_MAVEN_PLUGINS, MAVEN_COMPILER_PLUGIN);
        if (plugin != null) {
            return Optional.ofNullable((Xpp3Dom) plugin.getConfiguration());
        }
        return Optional.empty();
    }

    private Path resolveSerializedModelLocation() {
        final Path p = BootstrapUtils.resolveSerializedAppModelPath(Paths.get(project.getBuild().getDirectory()));
        p.toFile().deleteOnExit();
        return p;
    }

    private static final class PluginExec {
        final Plugin plugin;
        final String prefix;
        final PluginExecution execution;

        PluginExec(Plugin plugin, String prefix, PluginExecution execution) {
            this.plugin = plugin;
            this.prefix = prefix;
            this.execution = execution;
        }

        String getPluginId() {
            return plugin.getId();
        }

        String getPrefix() {
            return prefix == null ? plugin.getId() : prefix;
        }

        String getExecutionId() {
            return execution == null ? null : execution.getId();
        }
    }
}

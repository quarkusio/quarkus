package io.quarkus.gradle.application.tasks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.configuration.ConsoleOutput;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.deployment.internal.DeploymentRegistry;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import io.quarkus.deployment.dev.BuildOutputChanges;
import io.quarkus.deployment.dev.BuildOutputChangesDeliveryKind;
import io.quarkus.gradle.application.dsl.QuarkusApplicationDevExtensionJvmOptions;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlanner;
import io.quarkus.gradle.application.internal.config.EffectiveConfigRequest;
import io.quarkus.gradle.application.internal.dev.GradleDevOutputScope;
import io.quarkus.gradle.application.internal.dev.GradleDevOutputSnapshot;
import io.quarkus.gradle.application.internal.dev.GradleNativeDevModeLauncher;
import io.quarkus.gradle.application.internal.dev.QuarkusApplicationDevDeploymentHandle;
import io.quarkus.gradle.application.internal.dev.QuarkusApplicationDevDeployments;
import io.quarkus.gradle.application.internal.launch.ConsoleColorSupport;
import io.quarkus.gradle.application.model.QuarkusApplicationDevDebugMode;
import io.quarkus.gradle.application.model.QuarkusApplicationLaunchKind;
import io.quarkus.maven.dependency.ArtifactCoordsPattern;

/**
 * Runs Gradle-native Quarkus development mode.
 * <p>
 * The plugin registers {@code quarkusApplicationDev}; its continuous-testing specialization is registered as
 * {@code quarkusApplicationContinuousTest}. Both must be invoked with Gradle's {@code --continuous} option. Gradle owns
 * compilation and this task incrementally forwards successful output changes to one long-lived Quarkus session.
 * Command-line task options override the corresponding development DSL values for that invocation.
 * <p>
 * These tasks are neither configuration-cache compatible nor build-cacheable because they own a long-lived process and
 * deployment-registry state. The supported compatibility contract covers plugin-registered instances and the documented
 * task names, properties, and options. No compatibility commitment is made for direct construction, additional
 * registration, or subclassing.
 */
@DisableCachingByDefault(because = "Gradle-native dev mode is long-lived and does not produce reusable outputs")
public abstract class QuarkusApplicationDevTask extends QuarkusApplicationLaunchTask
        implements QuarkusApplicationLaunchOptions {

    private final ListProperty<String> commandLineEnvironmentVariables;

    /**
     * Creates a development task with empty argument collections, disabled continuous testing, inherited color
     * behavior, and no reusable up-to-date result.
     */
    public QuarkusApplicationDevTask() {
        commandLineEnvironmentVariables = getProject().getObjects().listProperty(String.class);
        getJvmArguments().convention(List.of());
        getApplicationArguments().convention(List.of());
        getModules().convention(List.of());
        getOpenJavaLang().convention(false);
        getCompilerArguments().convention(List.of());
        getTests().convention(List.of());
        getContinuousTesting().convention(false);
        getLegacyTestsOwned().convention(false);
        getEnvironmentVariables().convention(Map.of());
        commandLineEnvironmentVariables.convention(List.of());
        ConsoleOutput consoleOutput = getProject().getGradle().getStartParameter().getConsoleOutput();
        getForcePlainConsole().convention(getProviders().environmentVariable("NO_COLOR")
                .map(noColor -> ConsoleColorSupport.forcePlainConsole(consoleOutput, noColor))
                .orElse(ConsoleColorSupport.forcePlainConsole(consoleOutput, null)));
        getRuntimeForceColorSupport().convention(
                getProviders().systemProperty(ConsoleColorSupport.FORCE_COLOR_SUPPORT_PROPERTY));
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Returns whether this invocation uses Gradle continuous build.
     *
     * @return whether Gradle was invoked with {@code --continuous}
     */
    @Input
    public abstract Property<Boolean> getContinuousBuild();

    /**
     * Returns the application name passed to Quarkus development mode.
     *
     * @return the application name
     */
    @Input
    public abstract Property<String> getApplicationName();

    /**
     * Returns the application version passed to Quarkus development mode.
     *
     * @return the application version
     */
    @Input
    public abstract Property<String> getApplicationVersion();

    /**
     * Returns Quarkus build properties assembled from the root and development DSL.
     *
     * @return the Quarkus build properties
     */
    @Input
    public abstract MapProperty<String, String> getQuarkusBuildProperties();

    /**
     * Returns JVM arguments configured for the development-mode process through its fork options.
     *
     * @return the development JVM arguments
     */
    @Input
    public abstract ListProperty<String> getDevJvmArgs();

    /**
     * Returns additional application modules.
     *
     * @return the modules to add; empty by default
     */
    @Input
    @Option(description = "Modules to add to the application", option = "modules")
    public abstract ListProperty<String> getModules();

    /**
     * Returns whether the development JVM opens the {@code java.lang} package.
     *
     * @return whether to open {@code java.lang}; {@code false} by default
     */
    @Input
    @Option(description = "Open Java Lang module", option = "open-lang-package")
    public abstract Property<Boolean> getOpenJavaLang();

    /**
     * Returns additional compiler arguments used when Quarkus recompiles changed sources.
     *
     * @return additional compiler arguments; empty by default
     */
    @Input
    @Option(description = "Additional parameters to pass to javac when recompiling changed source files", option = "compiler-args")
    public abstract ListProperty<String> getCompilerArguments();

    /**
     * Returns continuous-test class or method filters. The {@code *} wildcard is supported.
     *
     * @return test filters; empty by default
     */
    @Input
    @Option(description = "Sets test class or method name to be included (for continuous testing), '*' is supported.", option = "tests")
    public abstract ListProperty<String> getTests();

    /**
     * Returns whether Quarkus continuous testing is enabled inside development mode.
     *
     * @return whether continuous testing is enabled; {@code false} for the dev task and {@code true} for the dedicated
     *         continuous-test task by default
     */
    @Input
    @Option(description = "Enables continuous testing inside Quarkus dev mode", option = "continuous-testing")
    public abstract Property<Boolean> getContinuousTesting();

    /**
     * Returns whether the legacy Quarkus plugin owns test execution in coexistence mode.
     * This is plugin wiring used to reject conflicting continuous-testing ownership.
     *
     * @return whether the legacy plugin owns tests
     */
    @Input
    public abstract Property<Boolean> getLegacyTestsOwned();

    /**
     * Returns system properties configured for the development-mode process.
     *
     * @return development-mode system properties
     */
    @Input
    public abstract MapProperty<String, String> getDevSystemProperties();

    /**
     * Returns the development process's working directory.
     *
     * @return the working directory, which must exist
     */
    @Internal
    @Option(description = "Working directory of the Quarkus dev-mode process", option = "working-directory")
    public abstract DirectoryProperty getWorkingDirectory();

    /**
     * Returns the normalized absolute working-directory path used as a modeled task input.
     *
     * @return the working-directory path
     */
    @Input
    public final String getWorkingDirectoryPath() {
        return getWorkingDirectory().get().getAsFile().toPath().toAbsolutePath().normalize().toString();
    }

    /**
     * Returns environment variables configured through the development DSL.
     *
     * @return the configured child-process environment
     */
    @Internal
    public abstract MapProperty<String, String> getEnvironmentVariables();

    /**
     * Returns a deterministic fingerprint of the effective child-process environment.
     *
     * @return the environment fingerprint
     */
    @Input
    public final String getEnvironmentVariablesFingerprint() {
        return QuarkusApplicationDevDeployments.environmentFingerprint(effectiveEnvironmentVariables());
    }

    /**
     * Replaces command-line environment entries for this invocation.
     * <p>
     * Each entry must use {@code NAME=VALUE} syntax. Command-line entries override same-named entries from the DSL.
     *
     * @param environmentVariables environment entries
     */
    @Option(description = "Adds or overrides a child-process environment entry as NAME=VALUE", option = "environment")
    public final void setCommandLineEnvironmentVariables(List<String> environmentVariables) {
        commandLineEnvironmentVariables.set(environmentVariables);
    }

    /**
     * Returns whether JVM debugging is enabled.
     *
     * @return the optional debug setting
     */
    @Input
    @Optional
    @Option(description = "Enables Quarkus JVM debugging", option = "quarkus-debug")
    public abstract Property<Boolean> getDebug();

    /**
     * Returns whether the debugger listens for or connects to a peer.
     *
     * @return the optional debug mode
     */
    @Input
    @Optional
    @Option(description = "Selects LISTEN or CONNECT debug mode", option = "debug-mode")
    public abstract Property<QuarkusApplicationDevDebugMode> getDebugMode();

    /**
     * Returns the debug host. A configured value must not be blank.
     *
     * @return the optional debug host
     */
    @Input
    @Optional
    @Option(description = "Sets the Quarkus debug host", option = "debug-host")
    public abstract Property<String> getDebugHost();

    /**
     * Returns the debug port. Zero or a negative value requests a random port; positive values must not exceed 65535.
     *
     * @return the optional debug port
     */
    @Input
    @Optional
    @Option(description = "Sets the Quarkus debug port; zero or a negative value selects a random port", option = "debug-port")
    public abstract Property<Integer> getDebugPort();

    /**
     * Returns whether the development JVM waits for a debugger before starting.
     *
     * @return the optional suspend setting
     */
    @Input
    @Optional
    @Option(description = "Suspends the Quarkus JVM until a debugger attaches", option = "suspend")
    public abstract Property<Boolean> getSuspend();

    /**
     * Returns whether Quarkus development mode forces the C2 compiler.
     *
     * @return the optional C2 setting
     */
    @Input
    @Optional
    @Option(description = "Forces the C2 compiler selection in Quarkus dev mode", option = "force-c2")
    public abstract Property<Boolean> getForceC2();

    /**
     * Returns controls for extension-provided development JVM options.
     *
     * @return extension JVM-option controls
     */
    @Nested
    public abstract QuarkusApplicationDevExtensionJvmOptions getExtensionJvmOptions();

    /**
     * Overrides the DSL setting that disables all extension-provided development JVM options.
     *
     * @param disableAll whether to disable all extension-provided options
     */
    @Option(description = "Disables all extension-provided dev-mode JVM options", option = "disable-all-extension-jvm-options")
    public final void setDisableAllExtensionJvmOptions(boolean disableAll) {
        getExtensionJvmOptions().getDisableAll().set(disableAll);
    }

    /**
     * Overrides the DSL artifact patterns selecting extensions whose development JVM options are disabled.
     *
     * @param patterns artifact-coordinate patterns accepted by Quarkus
     */
    @Option(description = "Disables dev-mode JVM options for matching extension artifact coordinates", option = "disable-extension-jvm-options-for")
    public final void setDisableExtensionJvmOptionsFor(List<String> patterns) {
        getExtensionJvmOptions().getDisableFor().set(patterns);
    }

    /**
     * Returns the Java toolchain launcher used for the development-mode process.
     *
     * @return the Java launcher
     */
    @Nested
    public abstract Property<JavaLauncher> getJavaLauncher();

    /**
     * Returns the project directory used to identify the long-lived session.
     *
     * @return the project directory
     */
    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    /**
     * Returns the Gradle build directory used for development-mode state.
     *
     * @return the build directory
     */
    @Internal
    public abstract DirectoryProperty getBuildDirectory();

    /**
     * Returns the receipt written when the long-lived development session closes.
     *
     * @return the close receipt
     */
    @Internal
    public abstract RegularFileProperty getCloseReceiptFile();

    /**
     * Returns local state containing the previous output snapshot used to calculate changes.
     *
     * @return the output snapshot file
     */
    @Internal
    public abstract RegularFileProperty getOutputSnapshotFile();

    /**
     * Returns the replay trigger whose changes request delivery of pending changes after live reload is re-enabled.
     *
     * @return the replay trigger file
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getReplayTriggerFile();

    /**
     * Returns the serialized development application model.
     *
     * @return the development application model
     */
    @Internal
    public abstract RegularFileProperty getApplicationModel();

    /**
     * Returns the serialized test application model, when continuous testing is enabled.
     *
     * @return the test application model
     */
    @Internal
    public abstract RegularFileProperty getTestApplicationModel();

    /**
     * Returns whether the child console must suppress color.
     *
     * @return whether to force plain console output
     */
    @Internal
    protected abstract Property<Boolean> getForcePlainConsole();

    /**
     * Returns the optional runtime force-color override.
     *
     * @return the runtime color override
     */
    @Internal
    protected abstract Property<String> getRuntimeForceColorSupport();

    /**
     * Returns main source directories used to discover effective configuration.
     *
     * @return the main source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceDirectories();

    /**
     * Returns test source directories passed to continuous-testing development mode.
     *
     * @return the test source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getTestSourceDirectories();

    /**
     * Returns the development-mode bootstrap classpath.
     *
     * @return the development-mode classpath
     */
    @Classpath
    public abstract ConfigurableFileCollection getDevModeClasspath();

    /**
     * Returns incrementally tracked main class outputs.
     *
     * @return main class output roots
     */
    @Incremental
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    public abstract ConfigurableFileCollection getApplicationClasses();

    /**
     * Returns incrementally tracked main resource outputs.
     *
     * @return main resource output roots
     */
    @Incremental
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    public abstract ConfigurableFileCollection getApplicationResources();

    /**
     * Returns incrementally tracked test class outputs.
     *
     * @return test class output roots
     */
    @Incremental
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    public abstract ConfigurableFileCollection getTestClasses();

    /**
     * Returns incrementally tracked test resource outputs.
     *
     * @return test resource output roots
     */
    @Incremental
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    public abstract ConfigurableFileCollection getTestResources();

    /**
     * Returns incrementally tracked class outputs from local project dependencies.
     *
     * @return dependency class output roots
     */
    @Incremental
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    public abstract ConfigurableFileCollection getDependencyClasses();

    /**
     * Returns incrementally tracked resource outputs from local project dependencies.
     *
     * @return dependency resource output roots
     */
    @Incremental
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    public abstract ConfigurableFileCollection getDependencyResources();

    /**
     * Returns incrementally tracked runtime JARs that have no consumable output variants.
     * Changes to these JARs require a development-session restart.
     *
     * @return runtime JAR inputs
     */
    @Incremental
    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    public abstract ConfigurableFileCollection getRuntimeJarsWithoutOutputVariants();

    /**
     * Returns the per-iteration diagnostic receipt.
     *
     * @return the iteration receipt
     */
    @OutputFile
    public abstract RegularFileProperty getReceiptFile();

    /**
     * Returns Gradle's deployment registry used to retain the development session across continuous-build iterations.
     *
     * @return the deployment registry
     */
    @Inject
    public abstract DeploymentRegistry getDeploymentRegistry();

    /**
     * Validates the invocation, starts or reuses the long-lived session, and forwards this iteration's output changes.
     *
     * @param inputChanges Gradle's incremental input changes
     * @throws IOException when session state or receipts cannot be read or written
     */
    @TaskAction
    public final void executeDevIteration(InputChanges inputChanges) throws IOException {
        warnIfLegacyAmbientConfigCaptureEnabled();
        validateTestOwnership();
        validateContinuousBuild();
        executeDeploymentDevIteration(inputChanges);
    }

    private void executeDeploymentDevIteration(InputChanges inputChanges) throws IOException {
        GradleNativeDevModeLauncher.Parameters launchParameters = launchParameters();
        Path replayTriggerFile = getReplayTriggerFile().get().getAsFile().toPath();
        String configFingerprint = QuarkusApplicationDevDeployments.configFingerprint(launchParameters, replayTriggerFile);
        String deploymentId = QuarkusApplicationDevDeployments.deploymentId(
                getProjectDirectory().get().getAsFile().toPath(), getPath());
        QuarkusApplicationDevDeployments.AcquiredHandle acquired = QuarkusApplicationDevDeployments.getOrStart(
                getDeploymentRegistry(), deploymentId, new QuarkusApplicationDevDeployments.Parameters(configFingerprint,
                        launchParameters, getCloseReceiptFile().get().getAsFile().toPath(), replayTriggerFile));
        QuarkusApplicationDevDeploymentHandle session = acquired.handle();
        long sequence = session.nextSequence();
        boolean ready = session.ready() && !acquired.started();
        List<GradleDevOutputTracker.IncrementalInput> inputs = incrementalInputs();
        GradleDevOutputTracker outputTracker = new GradleDevOutputTracker(snapshotRoots(inputs),
                getOutputSnapshotFile().get().getAsFile().toPath());
        GradleDevOutputTracker.ObservedDevChanges observed;
        BuildOutputChanges buildChanges;
        String outcome;
        if (acquired.restarted()) {
            observed = new GradleDevOutputTracker.ObservedDevChanges(false, true, List.of(), 0);
            buildChanges = outputTracker.recoveryRebaseline(sequence);
            String accepted = session.acceptReadyChangesOutcome(buildChanges);
            String delivered = session.deliverReadyChangesOutcome();
            outcome = "RECOVERY_REBASELINE," + accepted + "," + delivered;
        } else {
            observed = outputTracker.observe(inputChanges, ready, inputs);
            buildChanges = outputTracker.toBuildOutputChanges(sequence, observed);
            outcome = acceptChanges(session, buildChanges, ready, observed.incremental(), observed.runtimeJarChanges(),
                    getContinuousTesting().get());
        }
        writeReceipt(getReceiptFile().get().getAsFile().toPath(), sequence, observed.incremental(),
                observed.changes().size(), observed.runtimeJarChanges(), outcome, session.ready());
        if (getContinuousTesting().get()) {
            session.devUiUrl()
                    .ifPresent(url -> getLogger().lifecycle("Continuous Testing Dev UI: {}", url));
        }
    }

    private List<GradleDevOutputTracker.IncrementalInput> incrementalInputs() {
        return List.of(
                new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.MAIN_CLASSES, getApplicationClasses()),
                new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.MAIN_RESOURCES,
                        getApplicationResources()),
                new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.DEPENDENCY_CLASSES,
                        getDependencyClasses()),
                new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.DEPENDENCY_RESOURCES,
                        getDependencyResources()),
                new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.TEST_CLASSES, getTestClasses()),
                new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.TEST_RESOURCES, getTestResources()),
                new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.RUNTIME_JARS,
                        getRuntimeJarsWithoutOutputVariants()));
    }

    private static List<GradleDevOutputSnapshot.Root> snapshotRoots(
            List<GradleDevOutputTracker.IncrementalInput> inputs) {
        var roots = new ArrayList<GradleDevOutputSnapshot.Root>();
        for (GradleDevOutputTracker.IncrementalInput input : inputs) {
            for (var root : input.files().getFiles()) {
                roots.add(new GradleDevOutputSnapshot.Root(input.scope(), root));
            }
        }
        return List.copyOf(roots);
    }

    private void validateContinuousBuild() {
        if (!getContinuousBuild().getOrElse(false)) {
            throw new GradleException("Task '" + getPath()
                    + "' requires Gradle continuous build. Run it as './gradlew " + getPath()
                    + " --continuous' so Gradle owns source/resource compilation and can feed successful output changes "
                    + "to Quarkus dev mode.");
        }
    }

    private void validateTestOwnership() {
        boolean continuousTestOnly = getLaunchKind().getOrNull() == QuarkusApplicationLaunchKind.CONTINUOUS_TEST;
        if (continuousTestOnly && !getContinuousTesting().get()) {
            throw new GradleException("Task '" + getPath()
                    + "' always runs continuous testing and cannot be used with --no-continuous-testing. "
                    + "Use quarkusApplicationDev --no-continuous-testing for production-only dev mode.");
        }
        if ((continuousTestOnly || getContinuousTesting().get()) && getLegacyTestsOwned().get()) {
            throw new GradleException("Task '" + getPath()
                    + "' cannot enable continuous testing because legacy plugin 'io.quarkus' owns Quarkus test "
                    + "execution in coexistence mode. Use legacy 'quarkusTest', disable continuous testing for "
                    + "quarkusApplicationDev, or remove the legacy plugin.");
        }
    }

    private GradleNativeDevModeLauncher.Parameters launchParameters() {
        validateLaunchOptions();
        EffectiveConfigPlan effectiveConfig = effectiveConfig();
        Map<String, String> quarkusBuildProperties = new LinkedHashMap<>(effectiveConfig.buildSystemProperties());
        quarkusBuildProperties.putAll(getQuarkusBuildProperties().get());
        quarkusBuildProperties.putAll(gradleProperties());
        quarkusBuildProperties.putAll(systemProperties());
        String runtimeForceColorSupport = getRuntimeForceColorSupport().getOrNull();
        if (runtimeForceColorSupport != null) {
            quarkusBuildProperties.put(ConsoleColorSupport.FORCE_COLOR_SUPPORT_PROPERTY, runtimeForceColorSupport);
        }
        return new GradleNativeDevModeLauncher.Parameters(
                getJavaLauncher().get().getExecutablePath().getAsFile().toPath(),
                getApplicationModel().get().getAsFile().toPath(),
                getContinuousTesting().get() && getTestApplicationModel().isPresent()
                        ? getTestApplicationModel().get().getAsFile().toPath()
                        : null,
                getLaunchKind().get() == QuarkusApplicationLaunchKind.CONTINUOUS_TEST,
                getContinuousTesting().get(),
                getDevModeClasspath().getFiles(),
                getTestSourceDirectories().getFiles(),
                getTestClasses().getFiles(),
                getTestResources().getFiles(),
                getProjectDirectory().get().getAsFile().toPath(),
                getBuildDirectory().get().getAsFile().toPath(),
                getWorkingDirectory().get().getAsFile().toPath(),
                getApplicationName().get(),
                getApplicationVersion().get(),
                quarkusBuildProperties,
                getDevJvmArgs().get(),
                getJvmArguments().get(),
                getApplicationArguments().get(),
                getModules().get(),
                getOpenJavaLang().get(),
                getCompilerArguments().get(),
                getTests().get(),
                getForcePlainConsole().get(),
                getDevSystemProperties().get(),
                effectiveEnvironmentVariables(),
                getDebug().getOrNull(),
                getDebugMode().getOrNull(),
                getDebugHost().getOrNull(),
                getDebugPort().getOrNull(),
                getSuspend().getOrNull(),
                getForceC2().getOrNull(),
                getExtensionJvmOptions().getDisableAll().get(),
                getExtensionJvmOptions().getDisableFor().get());
    }

    private void validateLaunchOptions() {
        Path workingDirectory = getWorkingDirectory().get().getAsFile().toPath();
        validateWorkingDirectory(getPath(), workingDirectory);
        validateDebugHost(getPath(), getDebugHost().getOrNull());
        validateDebugPort(getPath(), getDebugPort().getOrNull());
        validateExtensionJvmOptionPatterns(getPath(), getExtensionJvmOptions().getDisableFor().get());
        effectiveEnvironmentVariables();
    }

    static void validateWorkingDirectory(String taskPath, Path workingDirectory) {
        if (!Files.isDirectory(workingDirectory)) {
            throw new GradleException("Task '" + taskPath + "' working directory '" + workingDirectory
                    + "' does not exist or is not a directory. Configure quarkusApplication.dev.workingDirectory "
                    + "or --working-directory with an existing directory.");
        }
    }

    static void validateDebugHost(String taskPath, String debugHost) {
        if (debugHost != null && debugHost.isBlank()) {
            throw new GradleException("Task '" + taskPath
                    + "' debug host must not be blank. Configure quarkusApplication.dev.debugHost or --debug-host.");
        }
    }

    static void validateDebugPort(String taskPath, Integer debugPort) {
        if (debugPort != null && debugPort > 65535) {
            throw new GradleException("Task '" + taskPath + "' debug port must be at most 65535, or zero/negative "
                    + "to select a random port. Configure quarkusApplication.dev.debugPort or --debug-port.");
        }
    }

    static void validateExtensionJvmOptionPatterns(String taskPath, List<String> patterns) {
        for (String pattern : patterns) {
            try {
                ArtifactCoordsPattern.of(pattern);
            } catch (RuntimeException e) {
                throw new GradleException("Task '" + taskPath + "' has invalid artifact pattern '" + pattern
                        + "' in quarkusApplication.dev.extensionJvmOptions.disableFor.", e);
            }
        }
    }

    final Map<String, String> effectiveEnvironmentVariables() {
        return mergeEnvironmentVariables(getEnvironmentVariables().get(), commandLineEnvironmentVariables.get());
    }

    static Map<String, String> mergeEnvironmentVariables(Map<String, String> configured,
            List<String> commandLineEntries) {
        Map<String, String> effective = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : configured.entrySet()) {
            validateEnvironmentEntry(entry.getKey(), entry.getValue(),
                    "quarkusApplication.dev.environmentVariables entry");
            effective.put(entry.getKey(), entry.getValue());
        }
        for (int i = 0; i < commandLineEntries.size(); i++) {
            String entry = commandLineEntries.get(i);
            if (entry == null) {
                throw new GradleException("--environment entry " + (i + 1) + " must not be null.");
            }
            int separator = entry.indexOf('=');
            if (separator < 0) {
                throw new GradleException("--environment entry " + (i + 1)
                        + " must use NAME=VALUE syntax.");
            }
            String name = entry.substring(0, separator);
            String value = entry.substring(separator + 1);
            validateEnvironmentEntry(name, value, "--environment entry " + (i + 1));
            effective.put(name, value);
        }
        return Map.copyOf(effective);
    }

    private static void validateEnvironmentEntry(String name, String value, String source) {
        if (name == null || name.isBlank()) {
            throw new GradleException(source + " has an empty or blank name.");
        }
        if (name.indexOf('=') >= 0) {
            throw new GradleException(source + " name must not contain '='.");
        }
        if (name.indexOf('\0') >= 0) {
            throw new GradleException(source + " name must not contain a NUL character.");
        }
        if (value == null) {
            throw new GradleException(source + " value must not be null.");
        }
        if (value.indexOf('\0') >= 0) {
            throw new GradleException(source + " value must not contain a NUL character.");
        }
    }

    private EffectiveConfigPlan effectiveConfig() {
        EffectiveConfigPlan plan = new EffectiveConfigPlanner().plan(
                new EffectiveConfigRequest(
                        Map.of(),
                        getApplicationName().get(),
                        getApplicationVersion().get(),
                        getSourceDirectories().getFiles(),
                        getQuarkusBuildProperties().get(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        gradleProperties(),
                        environmentVariables(),
                        systemProperties(),
                        Map.of(),
                        "dev"));
        if (!getLegacyAmbientConfigCapture().getOrElse(false)) {
            return plan;
        }
        return new EffectiveConfigPlan(
                plan.fullValues(),
                plan.quarkusWorkerValues(),
                plan.fullValues(),
                plan.descriptorShapeValues(),
                plan.diagnostics(),
                plan.externallyProvidedValuesOmitted(),
                plan.configSourceNames());
    }

    static String acceptChanges(QuarkusApplicationDevDeploymentHandle session, BuildOutputChanges buildChanges,
            boolean ready, boolean incremental, int runtimeJarChanges, boolean continuousTesting) {
        if (!ready) {
            if (continuousTesting) {
                BuildOutputChanges initialTestOutputs = new BuildOutputChanges(buildChanges.sequence(),
                        buildChanges.status(), buildChanges.failureKind(), buildChanges.mainClassChanges(),
                        buildChanges.mainResourceChanges(), buildChanges.testClassChanges(),
                        buildChanges.testResourceChanges(), buildChanges.failureSummary(), buildChanges.diagnosticsPath(),
                        buildChanges.userInitiated(), false);
                String accepted = session.acceptReadyChangesOutcome(initialTestOutputs);
                String delivered = session.deliverReadyChangesOutcome();
                return accepted + "," + delivered;
            }
            return session.acceptStartupBaselineOutcome(buildChanges);
        }
        if (buildChanges.deliveryKind() == BuildOutputChangesDeliveryKind.REBASELINE) {
            String accepted = session.acceptReadyChangesOutcome(buildChanges);
            String delivered = session.deliverReadyChangesOutcome();
            return accepted + "," + delivered;
        }
        if (!incremental || runtimeJarChanges > 0) {
            return session.acceptRestartRequiredOutcome(buildChanges.sequence());
        }
        String accepted = session.acceptReadyChangesOutcome(buildChanges);
        String delivered = session.deliverReadyChangesOutcome();
        return accepted + "," + delivered;
    }

    static void writeReceipt(Path receipt, long sequence, boolean incremental, int observedChanges, int runtimeJarChanges,
            String outcome, boolean ready) throws IOException {
        Files.createDirectories(receipt.getParent());
        Files.writeString(receipt,
                "sequence=" + sequence + "\n"
                        + "incremental=" + incremental + "\n"
                        + "observedChanges=" + observedChanges + "\n"
                        + "runtimeJarChanges=" + runtimeJarChanges + "\n"
                        + "sessionReady=" + ready + "\n"
                        + "outcome=" + outcome + "\n",
                StandardCharsets.UTF_8);
    }
}

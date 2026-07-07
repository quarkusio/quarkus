package io.quarkus.gradle.application.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.configuration.ConsoleOutput;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.execution.RunRequest;
import io.quarkus.gradle.application.internal.launch.ConsoleColorSupport;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

/**
 * Runs the packaged JVM application produced by a named build.
 * <p>
 * The task launches the package's recorded executable JAR with JVM and application arguments from the named-build DSL.
 * The optional run target uses the {@code quarkus.run.target} system-property convention. Remote-development server mode
 * is available only for mutable-JAR builds and can be enabled per invocation.
 * <p>
 * The task always executes and is neither configuration-cache compatible nor build-cacheable because it starts a
 * foreground process and may hold a transient command-line credential. The supported compatibility contract covers
 * plugin-registered instances and the documented task names, properties, and options. No compatibility commitment is
 * made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Quarkus run starts a foreground application process")
public abstract class QuarkusApplicationRunTask extends QuarkusApplicationBuildTask
        implements QuarkusApplicationLaunchOptions {

    private static final String QUARKUS_LAUNCH_DEVMODE = "QUARKUS_LAUNCH_DEVMODE";
    private static final String LIVE_RELOAD_PASSWORD = "quarkus.live-reload.password";
    private static final String DISABLE_CONSOLE_INPUT = "-Dquarkus.console.disable-input=true";
    private static final String DISABLE_CONTINUOUS_TESTING = "-Dquarkus.test.continuous-testing=disabled";

    private transient String liveReloadPassword;

    /**
     * Creates a run task with empty JVM/application arguments, the project directory as working directory, and remote
     * development disabled.
     */
    public QuarkusApplicationRunTask() {
        getJvmArguments().convention(List.of());
        getApplicationArguments().convention(List.of());
        ConsoleOutput consoleOutput = getProject().getGradle().getStartParameter().getConsoleOutput();
        getForcePlainConsole().convention(getProviders().environmentVariable("NO_COLOR")
                .map(noColor -> ConsoleColorSupport.forcePlainConsole(consoleOutput, noColor))
                .orElse(ConsoleColorSupport.forcePlainConsole(consoleOutput, null)));
        getRuntimeForceColorSupport().convention(
                getProviders().systemProperty(ConsoleColorSupport.FORCE_COLOR_SUPPORT_PROPERTY));
        getRunTarget().convention(getProviders().systemProperty("quarkus.run.target"));
        getEnableRemoteDev().convention(false);
        getWorkingDirectory().convention(getProject().getLayout().getProjectDirectory());
        getOutputs().upToDateWhen(task -> false);
        notCompatibleWithConfigurationCache(
                "Quarkus run starts a foreground process and may use transient command-line credentials.");
    }

    /**
     * Returns the package result descriptor that identifies the executable JAR.
     *
     * @return the package result file
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getPackageResultFile();

    /**
     * Returns the child process's working directory.
     *
     * @return the working directory; the project directory by default
     */
    @Internal
    public abstract DirectoryProperty getWorkingDirectory();

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
     * Returns the working-directory path modeled as a task input.
     *
     * @return the absolute working-directory path
     */
    @Input
    public String getWorkingDirectoryPath() {
        return getWorkingDirectory().get().getAsFile().getAbsolutePath();
    }

    /**
     * Returns the optional runner target.
     * Its convention is the {@code quarkus.run.target} system property.
     *
     * @return the optional run target
     */
    @Input
    @Optional
    public abstract Property<String> getRunTarget();

    /**
     * Returns whether this invocation starts a mutable-JAR package as a remote-development server.
     *
     * @return whether remote-development server mode is enabled; {@code false} by default
     */
    @Input
    public abstract Property<Boolean> getEnableRemoteDev();

    /**
     * Enables or disables remote-development server mode for this invocation.
     *
     * @param enableRemoteDev whether to enable remote-development server mode
     */
    @Option(description = "Start a mutable-jar package run as the remote-dev server side", option = "enable-remote-dev")
    public void enableRemoteDev(boolean enableRemoteDev) {
        getEnableRemoteDev().set(enableRemoteDev);
    }

    /**
     * Supplies the live-reload password for remote-development server mode.
     * <p>
     * The option requires {@code --enable-remote-dev}. Its value is transient task state and is passed only to the child
     * JVM.
     *
     * @param liveReloadPassword the live-reload password
     */
    @Option(description = "Remote-dev live-reload password for mutable-jar remote-dev server startup", option = "live-reload-password")
    public void liveReloadPassword(String liveReloadPassword) {
        this.liveReloadPassword = liveReloadPassword;
    }

    /**
     * Returns the package root used by the run operation.
     *
     * @return the package output directory
     */
    @Override
    @Internal
    public abstract DirectoryProperty getOutputDirectory();

    /**
     * Validates invocation options and launches the packaged application in the foreground.
     */
    @TaskAction
    public void runApplication() {
        validateRemoteDevOptions();
        Map<String, String> environment = runEnvironment();
        buildOperations().run(new RunRequest(
                buildRequest(Map.of()),
                getPackageResultFile().get().getAsFile().toPath(),
                java.util.Optional.ofNullable(getRunTarget().getOrNull()),
                runJvmArguments(),
                getApplicationArguments().get(),
                environment,
                getWorkingDirectory().get().getAsFile().toPath()));
    }

    private List<String> runJvmArguments() {
        List<String> configuredArguments = getJvmArguments().get();
        List<String> arguments = new ArrayList<>(configuredArguments.size() + 4);
        String configuredForceColorSupport = getRuntimeForceColorSupport().getOrNull();
        if (configuredForceColorSupport == null) {
            configuredForceColorSupport = getQuarkusBuildProperties().get()
                    .get(ConsoleColorSupport.FORCE_COLOR_SUPPORT_PROPERTY);
        }
        arguments.add(ConsoleColorSupport.jvmArgument(getForcePlainConsole().get(), configuredForceColorSupport));
        arguments.addAll(configuredArguments);
        if (!getEnableRemoteDev().get()) {
            return arguments;
        }
        arguments.add(DISABLE_CONSOLE_INPUT);
        arguments.add(DISABLE_CONTINUOUS_TESTING);
        if (liveReloadPassword != null && !liveReloadPassword.isBlank()) {
            arguments.add("-D" + LIVE_RELOAD_PASSWORD + "=" + liveReloadPassword);
        }
        return arguments;
    }

    private void validateRemoteDevOptions() {
        if (!getEnableRemoteDev().get() && liveReloadPassword != null && !liveReloadPassword.isBlank()) {
            throw new GradleException("Task '" + getPath()
                    + "' cannot use --live-reload-password without --enable-remote-dev.");
        }
    }

    private Map<String, String> runEnvironment() {
        if (!getEnableRemoteDev().get()) {
            return Map.of();
        }
        if (getBuildType().get() != QuarkusApplicationBuildType.MUTABLE_JAR) {
            throw new GradleException("Task '" + getPath()
                    + "' cannot use --enable-remote-dev because it runs a '" + getBuildType().get()
                    + "' package. Remote-dev server startup requires a mutable-jar run task.");
        }
        Map<String, String> environment = new java.util.LinkedHashMap<>();
        environment.put(QUARKUS_LAUNCH_DEVMODE, "true");
        return environment;
    }
}

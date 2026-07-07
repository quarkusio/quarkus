package io.quarkus.gradle.application.dsl;

import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import io.quarkus.gradle.application.model.QuarkusApplicationDevDebugMode;

/**
 * Configures the local {@code quarkusApplicationDev} launch and the shared launch settings used by
 * {@code quarkusApplicationContinuousTest}.
 * <p>
 * Dev mode requires Gradle continuous build. The project directory is the working-directory convention, collection
 * properties are empty by default, and continuous testing is disabled for the dev task by default. Debug and C2
 * settings are optional so Quarkus can retain its own defaults. Equivalent task command-line options override these DSL
 * values for the invocation.
 */
public abstract class QuarkusApplicationDev {

    private final QuarkusApplicationDevForkOptions forkOptions;
    private final QuarkusApplicationDevExtensionJvmOptions extensionJvmOptions;

    /**
     * Creates dev-mode configuration with project-directory and empty collection conventions.
     *
     * @param objects Gradle's object factory
     * @param layout the project layout
     */
    @Inject
    public QuarkusApplicationDev(ObjectFactory objects, ProjectLayout layout) {
        this.forkOptions = objects.newInstance(QuarkusApplicationDevForkOptions.class);
        this.extensionJvmOptions = objects.newInstance(QuarkusApplicationDevExtensionJvmOptions.class);
        getQuarkusBuildProperties().convention(Map.of());
        getContinuousTesting().convention(false);
        getWorkingDirectory().convention(layout.getProjectDirectory());
        getEnvironmentVariables().convention(Map.of());
    }

    /**
     * Returns Quarkus configuration applied to dev and continuous-test launches after root extension properties.
     *
     * @return the lazily configurable dev properties, empty by default
     */
    public abstract MapProperty<String, String> getQuarkusBuildProperties();

    /**
     * Enables Quarkus continuous testing inside {@code quarkusApplicationDev}.
     * <p>
     * The convention is {@code false}; {@code --continuous-testing} and {@code --no-continuous-testing} override it for
     * a task invocation. The dedicated continuous-test task is always enabled and rejects the disabling option.
     *
     * @return whether dev mode starts continuous testing
     */
    public abstract Property<Boolean> getContinuousTesting();

    /**
     * Returns the child process working directory.
     * <p>
     * The convention is the Gradle project directory. The selected path must exist and be a directory.
     *
     * @return the lazily configurable working directory
     */
    public abstract DirectoryProperty getWorkingDirectory();

    /**
     * Returns entries added to or replacing entries in the child process environment.
     * <p>
     * The convention is an empty map. Repeated {@code --environment=NAME=VALUE} task options override matching DSL
     * entries for that invocation.
     *
     * @return the lazily configurable child environment
     */
    public abstract MapProperty<String, String> getEnvironmentVariables();

    /**
     * Returns whether JVM debugging is enabled.
     *
     * @return the optional debug flag, unset by default
     */
    public abstract Property<Boolean> getDebug();

    /**
     * Returns whether the debugger listens for or connects to a debugger endpoint.
     *
     * @return the optional debug mode, unset by default
     */
    public abstract Property<QuarkusApplicationDevDebugMode> getDebugMode();

    /**
     * Returns the debugger host. A configured value must not be blank.
     *
     * @return the optional debug host, unset by default
     */
    public abstract Property<String> getDebugHost();

    /**
     * Returns the debugger port.
     * <p>
     * Values up to {@code 65535} are accepted; zero or a negative value requests a random port.
     *
     * @return the optional debug port, unset by default
     */
    public abstract Property<Integer> getDebugPort();

    /**
     * Returns whether the child JVM waits for a debugger before startup.
     *
     * @return the optional suspend flag, unset by default
     */
    public abstract Property<Boolean> getSuspend();

    /**
     * Returns whether Quarkus forces selection of the C2 compiler for dev mode.
     *
     * @return the optional C2 flag, unset by default
     */
    public abstract Property<Boolean> getForceC2();

    /**
     * Returns child-JVM arguments and system properties.
     *
     * @return the dev child fork options
     */
    public QuarkusApplicationDevForkOptions getForkOptions() {
        return forkOptions;
    }

    /**
     * Returns filtering for JVM options contributed by Quarkus extensions.
     *
     * @return the extension JVM-option filter
     */
    public QuarkusApplicationDevExtensionJvmOptions getExtensionJvmOptions() {
        return extensionJvmOptions;
    }

    /**
     * Configures child-JVM arguments and system properties.
     *
     * @param action the fork-options configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void forkOptions(Action<? super QuarkusApplicationDevForkOptions> action) {
        action.execute(forkOptions);
    }

    /**
     * Configures filtering of extension-contributed JVM options.
     *
     * @param action the filter configuration action
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void extensionJvmOptions(Action<? super QuarkusApplicationDevExtensionJvmOptions> action) {
        action.execute(extensionJvmOptions);
    }
}

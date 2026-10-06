package io.quarkus.gradle.application.dsl;

import java.util.List;
import java.util.Map;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

/**
 * Configures JVM process isolation for Quarkus build workers.
 * <p>
 * The root extension exposes separate instances for application build operations and code generation. Collection
 * properties are empty by default; assertions and debugging default to disabled; heap sizes and character encoding are
 * unset. Values remain provider-backed task inputs and are applied only when the corresponding worker process is
 * started.
 */
public abstract class QuarkusApplicationForkOptions {

    /**
     * Creates fork options with empty collection conventions and disabled assertions and debugging.
     */
    public QuarkusApplicationForkOptions() {
        getJvmArgs().convention(List.of());
        getSystemProperties().convention(Map.of());
        getEnvironment().convention(Map.of());
        getEnableAssertions().convention(false);
        getDebug().convention(false);
    }

    /**
     * Returns additional JVM arguments for the worker process.
     *
     * @return the lazily configurable JVM arguments
     */
    @Input
    public abstract ListProperty<String> getJvmArgs();

    /**
     * Returns system properties passed to the worker process.
     *
     * @return the lazily configurable system properties
     */
    @Input
    public abstract MapProperty<String, String> getSystemProperties();

    /**
     * Returns environment entries added to or replacing entries in the worker process environment.
     *
     * @return the lazily configurable environment entries
     */
    @Input
    public abstract MapProperty<String, String> getEnvironment();

    /**
     * Returns the optional minimum heap size using Gradle JVM-memory notation, for example {@code 512m}.
     *
     * @return the minimum heap size, unset by default
     */
    @Input
    @Optional
    public abstract Property<String> getMinHeapSize();

    /**
     * Returns the optional maximum heap size using Gradle JVM-memory notation, for example {@code 2g}.
     *
     * @return the maximum heap size, unset by default
     */
    @Input
    @Optional
    public abstract Property<String> getMaxHeapSize();

    /**
     * Returns whether the worker JVM enables assertions; the convention is {@code false}.
     *
     * @return whether assertions are enabled
     */
    @Input
    public abstract Property<Boolean> getEnableAssertions();

    /**
     * Returns whether Gradle starts the worker JVM with debugging enabled; the convention is {@code false}.
     *
     * @return whether worker debugging is enabled
     */
    @Input
    public abstract Property<Boolean> getDebug();

    /**
     * Returns the optional default character encoding for the worker JVM.
     *
     * @return the character encoding, unset by default
     */
    @Input
    @Optional
    public abstract Property<String> getDefaultCharacterEncoding();

    /**
     * Appends JVM arguments without realizing the property.
     *
     * @param args the arguments to append
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void jvmArgs(String... args) {
        getJvmArgs().addAll(args);
    }

    /**
     * Adds or replaces one worker system property.
     *
     * @param name the system-property name
     * @param value the system-property value
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void systemProperty(String name, String value) {
        getSystemProperties().put(name, value);
    }

    /**
     * Adds or replaces worker system properties.
     *
     * @param properties the system properties to add
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void systemProperties(Map<String, String> properties) {
        getSystemProperties().putAll(properties);
    }

    /**
     * Adds or replaces one worker environment entry.
     *
     * @param name the environment-variable name
     * @param value the environment-variable value
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void environment(String name, String value) {
        getEnvironment().put(name, value);
    }

    /**
     * Adds or replaces worker environment entries.
     *
     * @param environment the environment entries to add
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void environment(Map<String, String> environment) {
        getEnvironment().putAll(environment);
    }
}

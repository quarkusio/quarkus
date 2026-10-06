package io.quarkus.gradle.application.dsl;

import java.util.List;
import java.util.Map;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;

/**
 * Configures JVM arguments and system properties for a development operation.
 * <p>
 * In {@link QuarkusApplicationDev} these values configure the long-lived local child process. In
 * {@link QuarkusApplicationRemoteDev} they extend the worker JVM used to build the internal mutable JAR. Both
 * collections are empty by default. Values are captured lazily by the owning task; equivalent command-line launch
 * options take precedence where the task exposes them.
 */
public abstract class QuarkusApplicationDevForkOptions {

    /**
     * Creates empty JVM-argument and system-property conventions.
     */
    public QuarkusApplicationDevForkOptions() {
        getJvmArgs().convention(List.of());
        getSystemProperties().convention(Map.of());
    }

    /**
     * Returns additional arguments for the child JVM.
     *
     * @return the lazily configurable JVM arguments
     */
    public abstract ListProperty<String> getJvmArgs();

    /**
     * Returns system properties passed to the child JVM.
     *
     * @return the lazily configurable system properties
     */
    public abstract MapProperty<String, String> getSystemProperties();

    /**
     * Appends child-JVM arguments without replacing existing entries.
     *
     * @param args the arguments to append
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void jvmArgs(String... args) {
        getJvmArgs().addAll(args);
    }

    /**
     * Adds or replaces one child-JVM system property.
     *
     * @param name the system-property name
     * @param value the system-property value
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void systemProperty(String name, String value) {
        getSystemProperties().put(name, value);
    }
}

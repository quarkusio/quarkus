package io.quarkus.gradle.application.tasks;

import java.util.List;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.options.Option;

/**
 * Shared modeled arguments and command-line overrides for plugin-registered launch tasks.
 * This package-private mix-in is implementation wiring rather than a typed user entry point.
 */
interface QuarkusApplicationLaunchOptions {

    /**
     * Returns JVM arguments for the launched process.
     *
     * @return the JVM arguments; empty by default
     */
    @Input
    ListProperty<String> getJvmArguments();

    /**
     * Returns arguments passed to the launched Quarkus application.
     *
     * @return the application arguments; empty by default
     */
    @Input
    ListProperty<String> getApplicationArguments();

    /**
     * Replaces JVM arguments for this invocation.
     *
     * @param jvmArguments JVM arguments
     */
    @Option(description = "Set JVM arguments", option = "jvm-args")
    default void setJvmArgs(List<String> jvmArguments) {
        getJvmArguments().set(jvmArguments);
    }

    /**
     * Replaces Quarkus application arguments for this invocation.
     *
     * @param applicationArguments application arguments
     */
    @Option(description = "Set application arguments", option = "quarkus-args")
    default void setQuarkusArgs(List<String> applicationArguments) {
        getApplicationArguments().set(applicationArguments);
    }
}

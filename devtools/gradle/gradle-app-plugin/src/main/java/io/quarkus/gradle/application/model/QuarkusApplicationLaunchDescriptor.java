package io.quarkus.gradle.application.model;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

/**
 * Identifies one long-lived application launch requested from Gradle.
 *
 * @param name an optional named launch; empty identifies the default launch
 * @param kind the launch lifecycle
 */
public record QuarkusApplicationLaunchDescriptor(Optional<String> name,
        QuarkusApplicationLaunchKind kind) {

    /**
     * Creates and validates a launch descriptor.
     */
    public QuarkusApplicationLaunchDescriptor {
        requireNonNull(name, "name");
        if (kind == null) {
            throw new IllegalArgumentException("Quarkus application launch descriptor requires a kind");
        }
    }

    /**
     * Identifies the default continuous-test launch.
     *
     * @return the descriptor
     */
    public static QuarkusApplicationLaunchDescriptor continuousTest() {
        return new QuarkusApplicationLaunchDescriptor(Optional.empty(), QuarkusApplicationLaunchKind.CONTINUOUS_TEST);
    }

    /**
     * Identifies a named continuous-test launch.
     *
     * @param name the launch name
     * @return the descriptor
     */
    public static QuarkusApplicationLaunchDescriptor continuousTest(String name) {
        return new QuarkusApplicationLaunchDescriptor(Optional.of(name), QuarkusApplicationLaunchKind.CONTINUOUS_TEST);
    }
}

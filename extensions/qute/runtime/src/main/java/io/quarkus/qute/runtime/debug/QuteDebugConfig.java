package io.quarkus.qute.runtime.debug;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface QuteDebugConfig {

    /**
     * Enables or disables the Qute debug mode. This feature is experimental.
     * <p>
     * When enabled, Qute templates can be debugged directly at runtime.
     * This includes the ability to:
     * </p>
     * <ul>
     * <li>Set breakpoints inside templates</li>
     * <li>Inspect the stack trace of visited template nodes during rendering</li>
     * <li>Evaluate expressions in the current Qute context</li>
     * </ul>
     *
     * <p>
     * This mode is intended for development and troubleshooting purposes.
     * </p>
     *
     * <p>
     * <strong>Default value:</strong> {@code true}
     * </p>
     *
     * <p>
     * Example configuration:
     * </p>
     *
     * <pre>
     * quarkus.qute.debug.enabled = false
     * </pre>
     *
     * @return {@code true} if Qute debug mode is active, {@code false} otherwise.
     */
    @WithDefault("true")
    boolean enabled();
}

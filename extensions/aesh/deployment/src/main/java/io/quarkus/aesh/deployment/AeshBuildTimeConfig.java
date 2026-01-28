package io.quarkus.aesh.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time configuration for the Aesh extension.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.aesh")
public interface AeshBuildTimeConfig {

    /**
     * The execution mode for the Aesh extension.
     * <ul>
     * <li>{@code auto} - Automatically detect the mode based on annotations.
     * Uses runtime mode for single command or @GroupCommandDefinition,
     * console mode for multiple @CliCommand annotations.</li>
     * <li>{@code runtime} - Use AeshRuntimeRunner for single command execution (like picocli).</li>
     * <li>{@code console} - Use AeshConsoleRunner for interactive shell mode.</li>
     * </ul>
     */
    @WithDefault("auto")
    String mode();

    /**
     * Whether to start the local console (stdin/stdout) for interactive CLI access.
     * <p>
     * When not set, auto-detected: the local console is started if no remote transport
     * (SSH, WebSocket) is present, and skipped if a remote transport is present.
     * <p>
     * Set to {@code true} to force starting the local console alongside remote transports.
     * Set to {@code false} to disable the local console even without remote transports
     * (useful when embedding commands in a server application).
     */
    Optional<Boolean> startConsole();
}

package io.quarkus.aesh.runtime;

import java.util.List;

/**
 * Runtime context containing build-time metadata about discovered aesh commands
 * and the resolved execution mode.
 * <p>
 * This is produced as a synthetic CDI bean by {@link AeshRecorder} during augmentation.
 * It can be injected by application code or other extensions that need to inspect
 * the CLI configuration.
 */
public interface AeshContext {

    /**
     * Returns metadata for all discovered CLI commands.
     */
    List<AeshCommandMetadata> getCommands();

    /**
     * Returns the resolved execution mode. This is never {@link AeshMode#auto} --
     * the mode is resolved during build time.
     */
    AeshMode getMode();
}

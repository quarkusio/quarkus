package io.quarkus.aesh.runtime;

import java.util.List;

/**
 * Runtime context containing build-time metadata about discovered aesh commands
 * and the resolved execution mode.
 * <p>
 * This is produced as a synthetic CDI bean from a service registered during augmentation.
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

    /**
     * Returns the fully qualified class name of the top command in runtime mode,
     * or {@code null} in console mode.
     */
    String getTopCommandClassName();

    static AeshContext of(List<AeshCommandMetadata> commands, AeshMode mode, String topCommandClassName) {
        return new AeshContext() {
            @Override
            public List<AeshCommandMetadata> getCommands() {
                return commands;
            }

            @Override
            public AeshMode getMode() {
                return mode;
            }

            @Override
            public String getTopCommandClassName() {
                return topCommandClassName;
            }
        };
    }
}

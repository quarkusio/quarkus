package io.quarkus.aesh.runtime;

import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;

/**
 * Factory for creating CommandRegistryBuilder instances for console mode.
 * This is used when the application runs in interactive shell mode with multiple commands.
 */
public interface CliCommandRegistryFactory {

    /**
     * Create an AeshCommandRegistryBuilder containing all discovered commands.
     *
     * @return an AeshCommandRegistryBuilder with all commands registered
     */
    AeshCommandRegistryBuilder<CommandInvocation> create();
}

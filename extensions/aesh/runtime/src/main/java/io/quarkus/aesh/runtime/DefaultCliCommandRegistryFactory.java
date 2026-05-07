package io.quarkus.aesh.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import org.aesh.command.Command;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;

import io.quarkus.aesh.runtime.annotations.CliCommand;

/**
 * Default implementation of CliCommandRegistryFactory that resolves commands from CDI.
 * It collects all beans annotated with @CliCommand and registers them in the command registry.
 */
@ApplicationScoped
public class DefaultCliCommandRegistryFactory implements CliCommandRegistryFactory {

    private final Instance<Command<CommandInvocation>> commands;

    public DefaultCliCommandRegistryFactory(@CliCommand Instance<Command<CommandInvocation>> commands) {
        this.commands = commands;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AeshCommandRegistryBuilder<CommandInvocation> create() {
        AeshCommandRegistryBuilder<CommandInvocation> builder = AeshCommandRegistryBuilder.<CommandInvocation> builder();
        builder.containerBuilder(new AeshCdiCommandContainerBuilder<>());

        for (Command<CommandInvocation> command : commands) {
            try {
                // Register command instances directly to ensure CDI injection is available
                builder.command(command);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to register command: " + command.getClass().getName(), e);
            }
        }

        return builder;
    }
}

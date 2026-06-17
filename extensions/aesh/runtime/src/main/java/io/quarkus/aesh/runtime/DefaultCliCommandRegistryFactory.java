package io.quarkus.aesh.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import org.aesh.command.Command;
import org.aesh.command.DefaultValueProvider;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;

/**
 * Default implementation of CliCommandRegistryFactory that resolves commands from CDI.
 * It collects all {@code Command} beans and registers them in the command registry
 * for console (interactive shell) mode.
 */
@ApplicationScoped
public class DefaultCliCommandRegistryFactory implements CliCommandRegistryFactory {

    private final Instance<Command<? extends CommandInvocation>> commands;
    private final Instance<DefaultValueProvider> defaultValueProvider;

    @SuppressWarnings("unchecked")
    public DefaultCliCommandRegistryFactory(Instance<Command<? extends CommandInvocation>> commands,
            Instance<DefaultValueProvider> defaultValueProvider) {
        this.commands = commands;
        this.defaultValueProvider = defaultValueProvider;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AeshCommandRegistryBuilder create() {
        AeshCommandRegistryBuilder builder = AeshCommandRegistryBuilder.builder();
        builder.containerBuilder(new AeshCdiCommandContainerBuilder<>());

        if (defaultValueProvider.isResolvable()) {
            builder.defaultValueProvider(defaultValueProvider.get());
        }

        for (Command command : commands) {
            try {
                builder.command(command);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to register command: " + command.getClass().getName(), e);
            }
        }

        return builder;
    }
}

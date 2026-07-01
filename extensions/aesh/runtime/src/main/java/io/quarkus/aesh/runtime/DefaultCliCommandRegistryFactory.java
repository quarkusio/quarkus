package io.quarkus.aesh.runtime;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import org.aesh.command.Command;
import org.aesh.command.DefaultValueProvider;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;

import io.quarkus.arc.ClientProxy;

/**
 * Default implementation of CliCommandRegistryFactory that resolves commands from CDI.
 * It collects all {@code Command} beans and registers only top-level commands
 * in the command registry. Sub-commands (declared via {@code groupCommands}
 * annotation attribute or detected as {@code @Inject} fields on
 * {@code GroupCommand} implementations) are excluded -- they are registered
 * automatically by aesh when processing their parent group command.
 */
@ApplicationScoped
public class DefaultCliCommandRegistryFactory implements CliCommandRegistryFactory {

    private final Instance<Command<? extends CommandInvocation>> commands;
    private final Instance<DefaultValueProvider> defaultValueProvider;
    private final AeshContext aeshContext;

    @SuppressWarnings("unchecked")
    public DefaultCliCommandRegistryFactory(Instance<Command<? extends CommandInvocation>> commands,
            Instance<DefaultValueProvider> defaultValueProvider,
            AeshContext aeshContext) {
        this.commands = commands;
        this.defaultValueProvider = defaultValueProvider;
        this.aeshContext = aeshContext;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AeshCommandRegistryBuilder create() {
        AeshCommandRegistryBuilder builder = AeshCommandRegistryBuilder.builder();
        builder.containerBuilder(new AeshCdiCommandContainerBuilder<>());

        if (defaultValueProvider.isResolvable()) {
            builder.defaultValueProvider(defaultValueProvider.get());
        }

        // Collect all subcommand class names so we only register top-level commands.
        // Subcommands are registered by aesh when processing their parent group.
        Set<String> subCommandClasses = new HashSet<>();
        for (AeshCommandMetadata meta : aeshContext.getCommands()) {
            subCommandClasses.addAll(meta.getSubCommandClassNames());
        }

        for (Command command : commands) {
            String className = unwrapClassName(command);
            if (subCommandClasses.contains(className)) {
                continue;
            }
            try {
                builder.command(command);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to register command: " + className, e);
            }
        }

        return builder;
    }

    /**
     * Unwrap CDI proxy class names to get the actual bean class name.
     */
    private static String unwrapClassName(Object bean) {
        if (bean instanceof ClientProxy) {
            return bean.getClass().getSuperclass().getName();
        }
        return bean.getClass().getName();
    }
}

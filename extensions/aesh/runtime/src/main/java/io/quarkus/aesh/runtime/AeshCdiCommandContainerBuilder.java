package io.quarkus.aesh.runtime;

import java.lang.reflect.Field;
import java.util.List;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.util.ReflectionUtil;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InstanceHandle;

/**
 * A CDI-aware CommandContainerBuilder that integrates with Quarkus Arc container.
 * This builder extends the default Aesh builder to resolve command instances from CDI first,
 * falling back to reflection-based instantiation if the command is not a CDI bean.
 * <p>
 * Sub-commands in {@code @GroupCommandDefinition} are created by Aesh via reflection.
 * After the command tree is built, this builder injects CDI dependencies into those
 * sub-command instances so that {@code @Inject} fields are populated.
 * <p>
 * Aesh also creates service providers (completers, validators, converters, activators)
 * via reflection. This builder injects CDI dependencies into those instances as well.
 *
 * @param <CI> the command invocation type
 */
public class AeshCdiCommandContainerBuilder<CI extends CommandInvocation>
        extends AeshCommandContainerBuilder<CI> {

    private static final Logger LOG = Logger.getLogger(AeshCdiCommandContainerBuilder.class);

    @Override
    public CommandContainer<CI> create(Class<? extends Command> commandClass) throws CommandLineParserException {
        Class<? extends Command> actualClass = getBeanClass(commandClass);
        Command<CI> commandInstance = createCommandInstance(actualClass);
        CommandContainer<CI> container = super.create(commandInstance);
        injectCdiIntoCommandTree(container.getParser());
        return container;
    }

    @Override
    public CommandContainer<CI> create(Command command) throws CommandLineParserException {
        CommandContainer<CI> container = super.create(command);
        // After Aesh builds the command tree (including sub-commands via reflection),
        // walk the tree and inject CDI dependencies into sub-command instances
        // and into service providers (completers, validators, converters, activators)
        injectCdiIntoCommandTree(container.getParser());
        return container;
    }

    /**
     * Recursively walks the command parser tree and injects CDI beans into
     * sub-command instances and option service providers.
     */
    private void injectCdiIntoCommandTree(CommandLineParser<CI> parser) {
        // Inject CDI into service providers on this command's options
        injectCdiIntoOptionProviders(parser.getProcessedCommand());

        List<CommandLineParser<CI>> children = parser.getAllChildParsers();
        if (children == null) {
            return;
        }
        for (CommandLineParser<CI> child : children) {
            Command<CI> command = child.getCommand();
            if (command != null) {
                injectFields(command);
            }
            // Recurse into child commands
            injectCdiIntoCommandTree(child);
        }
    }

    /**
     * Injects CDI beans into service providers (completers, validators, converters,
     * activators) that Aesh created via reflection for each processed option.
     */
    private void injectCdiIntoOptionProviders(ProcessedCommand<?, ?> processedCommand) {
        for (ProcessedOption option : processedCommand.getOptions()) {
            injectCdiIntoOptionServiceProviders(option);
        }
        if (processedCommand.getArgument() != null) {
            injectCdiIntoOptionServiceProviders(processedCommand.getArgument());
        }
        if (processedCommand.getArguments() != null) {
            injectCdiIntoOptionServiceProviders(processedCommand.getArguments());
        }
    }

    private void injectCdiIntoOptionServiceProviders(ProcessedOption option) {
        if (option.completer() != null) {
            injectFields(option.completer());
        }
        if (option.validator() != null) {
            injectFields(option.validator());
        }
        if (option.converter() != null) {
            injectFields(option.converter());
        }
        if (option.activator() != null) {
            injectFields(option.activator());
        }
    }

    /**
     * Injects CDI bean instances into fields annotated with {@code @Inject}.
     */
    private void injectFields(Object instance) {
        Class<?> clazz = instance.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    InstanceHandle<?> handle = Arc.container().instance(field.getType());
                    if (handle.isAvailable()) {
                        try {
                            field.setAccessible(true);
                            field.set(instance, handle.get());
                        } catch (Exception e) {
                            throw new RuntimeException(
                                    "Failed to inject CDI bean into field " + field.getName()
                                            + " of " + instance.getClass().getName(),
                                    e);
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Command> getBeanClass(Class<? extends Command> commandClass) {
        if (ClientProxy.class.isAssignableFrom(commandClass)) {
            Class<?> superclass = commandClass.getSuperclass();
            if (Command.class.isAssignableFrom(superclass)) {
                return (Class<? extends Command>) superclass;
            }
        }
        return commandClass;
    }

    @SuppressWarnings("unchecked")
    private <T extends Command> T createCommandInstance(Class<? extends Command> commandClass) {
        try {
            InstanceHandle<? extends Command> instance = Arc.container().instance(commandClass);
            if (instance.isAvailable()) {
                return (T) instance.get();
            }
        } catch (Exception e) {
            LOG.debugf(e, "CDI lookup failed for %s, falling back to reflection", commandClass.getName());
        }
        return (T) ReflectionUtil.newInstance(commandClass);
    }
}

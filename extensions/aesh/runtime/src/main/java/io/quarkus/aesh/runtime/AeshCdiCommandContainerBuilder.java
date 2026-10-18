package io.quarkus.aesh.runtime;

import org.aesh.command.Command;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.AeshCommandLineParser;
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
 * <p>
 * Resolves command instances from CDI first, falling back to reflection if the
 * command is not a CDI bean. Sets a child resolver on the parser so that
 * sub-commands in group commands are also resolved through this builder.
 * Replaces aesh-created service providers (completers, validators, converters,
 * activators) with CDI-managed beans where available.
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
        configureParser(container.getParser());
        return container;
    }

    @Override
    public CommandContainer<CI> create(Command command) throws CommandLineParserException {
        CommandContainer<CI> container = super.create(command);
        configureParser(container.getParser());
        return container;
    }

    /**
     * Configures the parser to use CDI for resolving child commands and
     * replaces service providers with CDI-managed beans on this command's options.
     */
    private void configureParser(CommandLineParser<CI> parser) {
        // Set child resolver so sub-commands are resolved through this CDI-aware builder
        if (parser instanceof AeshCommandLineParser<CI> aeshParser) {
            aeshParser.setChildResolver(childClass -> {
                try {
                    return this.create(childClass);
                } catch (CommandLineParserException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Replace service providers with CDI-managed beans
        replaceCdiServiceProviders(parser.getProcessedCommand());
    }

    /**
     * Replaces aesh-created service providers with CDI-managed beans
     * for all options, arguments, and argument lists.
     */
    @SuppressWarnings("unchecked")
    private void replaceCdiServiceProviders(ProcessedCommand<?, ?> processedCommand) {
        for (ProcessedOption option : processedCommand.getOptions()) {
            replaceCdiServiceProviders(option);
        }
        if (processedCommand.getArgument() != null) {
            replaceCdiServiceProviders(processedCommand.getArgument());
        }
        if (processedCommand.getArguments() != null) {
            replaceCdiServiceProviders(processedCommand.getArguments());
        }
    }

    @SuppressWarnings("unchecked")
    private void replaceCdiServiceProviders(ProcessedOption option) {
        if (option.completer() != null) {
            InstanceHandle<?> handle = Arc.container().instance(option.completer().getClass());
            if (handle.isAvailable()) {
                option.setCompleter((org.aesh.command.completer.OptionCompleter) handle.get());
            }
        }
        if (option.validator() != null) {
            InstanceHandle<?> handle = Arc.container().instance(option.validator().getClass());
            if (handle.isAvailable()) {
                option.setValidator((org.aesh.command.validator.OptionValidator) handle.get());
            }
        }
        if (option.converter() != null) {
            InstanceHandle<?> handle = Arc.container().instance(option.converter().getClass());
            if (handle.isAvailable()) {
                option.setConverter((org.aesh.command.converter.Converter) handle.get());
            }
        }
        if (option.activator() != null) {
            InstanceHandle<?> handle = Arc.container().instance(option.activator().getClass());
            if (handle.isAvailable()) {
                option.setActivator((org.aesh.command.activator.OptionActivator) handle.get());
            }
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
            LOG.warnf(e, "CDI lookup failed for %s, falling back to reflection", commandClass.getName());
        }
        return (T) ReflectionUtil.newInstance(commandClass);
    }
}

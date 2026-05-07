package io.quarkus.aesh.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.Command;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;

import io.quarkus.aesh.runtime.annotations.TopCommand;
import io.quarkus.arc.Arc;

/**
 * Default implementation of AeshRuntimeRunnerFactory that resolves the top command from CDI.
 * Uses {@link AeshCdiCommandContainerBuilder} to ensure CDI injection works in sub-commands.
 */
@ApplicationScoped
public class DefaultAeshRuntimeRunnerFactory implements AeshRuntimeRunnerFactory {

    private final Instance<Object> topCommand;
    private final CliConfig configuration;

    public DefaultAeshRuntimeRunnerFactory(@TopCommand Instance<Object> topCommand,
            CliConfig configuration) {
        this.topCommand = topCommand;
        this.configuration = configuration;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AeshRuntimeRunner create() {
        Command<?> commandInstance = resolveTopCommand();
        if (commandInstance != null) {
            // Build a command registry with the CDI-aware container builder
            // so that sub-commands in @GroupCommandDefinition get CDI injection
            AeshCommandRegistryBuilder<CommandInvocation> registryBuilder = AeshCommandRegistryBuilder
                    .<CommandInvocation> builder();
            registryBuilder.containerBuilder(new AeshCdiCommandContainerBuilder<>());
            try {
                registryBuilder.command((Command) commandInstance);
            } catch (Exception e) {
                throw new RuntimeException("Failed to register command: " + commandInstance.getClass().getName(), e);
            }

            CommandRuntime<CommandInvocation> runtime = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                    .commandRegistry(registryBuilder.create())
                    .build();

            return AeshRuntimeRunner.builder().commandRuntime(runtime);
        }
        throw new IllegalStateException(
                "Unable to find top command. Please annotate a command class with @TopCommand or set quarkus.aesh.top-command property.");
    }

    @SuppressWarnings("unchecked")
    private Command<?> resolveTopCommand() {
        if (configuration.topCommand().isPresent()) {
            String topCommandName = configuration.topCommand().get();
            Instance<Object> namedBean = topCommand.select(NamedLiteral.of(topCommandName));
            if (namedBean.isResolvable()) {
                return asCommand(namedBean.get());
            }
            try {
                Class<?> commandClass = Thread.currentThread().getContextClassLoader().loadClass(topCommandName);
                if (!Command.class.isAssignableFrom(commandClass)) {
                    throw new IllegalStateException(
                            "Top command must implement org.aesh.command.Command interface: " + topCommandName);
                }
                return (Command<?>) Arc.container().instance(commandClass).get();
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to find or load top command: " + topCommandName, e);
            }
        }
        if (topCommand.isResolvable()) {
            return asCommand(topCommand.get());
        }
        return null;
    }

    private Command<?> asCommand(Object bean) {
        if (!(bean instanceof Command)) {
            throw new IllegalStateException(
                    "Top command must implement org.aesh.command.Command interface: " + bean.getClass().getName());
        }
        return (Command<?>) bean;
    }
}

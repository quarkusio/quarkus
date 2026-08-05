package io.quarkus.aesh.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.DefaultValueProvider;

import io.quarkus.arc.Arc;

/**
 * Default implementation of AeshRuntimeRunnerFactory that resolves the top command
 * from the build-time detected top command class name (via {@link AeshContext})
 * or from the {@code quarkus.aesh.top-command} config property.
 */
@ApplicationScoped
public class DefaultAeshRuntimeRunnerFactory implements AeshRuntimeRunnerFactory {

    private final Instance<DefaultValueProvider> defaultValueProvider;
    private final CliConfig configuration;
    private final AeshContext aeshContext;

    public DefaultAeshRuntimeRunnerFactory(Instance<DefaultValueProvider> defaultValueProvider,
            CliConfig configuration,
            AeshContext aeshContext) {
        this.defaultValueProvider = defaultValueProvider;
        this.configuration = configuration;
        this.aeshContext = aeshContext;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AeshRuntimeRunner create() {
        Command<?> commandInstance = resolveTopCommand();
        if (commandInstance != null) {
            AeshRuntimeRunner runner = AeshRuntimeRunner.builder()
                    .containerBuilder(new AeshCdiCommandContainerBuilder<>())
                    .command(commandInstance);
            if (defaultValueProvider.isResolvable()) {
                runner.defaultValueProvider(defaultValueProvider.get());
            }
            return runner;
        }
        throw new IllegalStateException(
                "Unable to find top command. Ensure you have a @CommandDefinition class "
                        + "or set the quarkus.aesh.top-command property.");
    }

    @SuppressWarnings("unchecked")
    private Command<?> resolveTopCommand() {
        // 1. Explicit config property overrides everything
        if (configuration.topCommand().isPresent()) {
            String topCommandName = configuration.topCommand().get();
            return loadCommand(topCommandName);
        }

        // 2. Use the build-time detected top command
        String topClassName = aeshContext.getTopCommandClassName();
        if (topClassName != null) {
            return loadCommand(topClassName);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Command<?> loadCommand(String className) {
        try {
            Class<?> commandClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            if (!Command.class.isAssignableFrom(commandClass)) {
                throw new IllegalStateException(
                        "Top command must implement org.aesh.command.Command interface: " + className);
            }
            var handle = Arc.container().instance(commandClass);
            if (handle.isAvailable()) {
                return (Command<?>) handle.get();
            }
            // Not a CDI bean — fall back to direct instantiation
            return (Command<?>) commandClass.getConstructor().newInstance();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to find or load top command: " + className, e);
        }
    }
}

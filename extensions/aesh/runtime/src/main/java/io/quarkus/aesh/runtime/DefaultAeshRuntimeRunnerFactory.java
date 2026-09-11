package io.quarkus.aesh.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.DefaultValueProvider;

/**
 * Default implementation of AeshRuntimeRunnerFactory that resolves the top command
 * from the build-time detected top command class name (via {@link AeshContext})
 * or from the {@code quarkus.aesh.top-command} config property.
 * <p>
 * The top command is passed to {@link AeshRuntimeRunner} as a {@link Class},
 * not a pre-constructed instance. With aesh lazy startup (the default), the
 * command is constructed during {@code execute()} via
 * {@link AeshCdiCommandContainerBuilder}, which resolves CDI beans. This means
 * construction failures (missing constructor, invalid definition) surface at
 * execution time rather than at factory creation time — only class loading
 * and structural validation happen eagerly here.
 */
@ApplicationScoped
public class DefaultAeshRuntimeRunnerFactory implements AeshRuntimeRunnerFactory {

    private final Instance<DefaultValueProvider> defaultValueProvider;
    private final Instance<CliSettings> customizers;
    private final CliConfig configuration;
    private final AeshContext aeshContext;

    public DefaultAeshRuntimeRunnerFactory(Instance<DefaultValueProvider> defaultValueProvider,
            Instance<CliSettings> customizers,
            CliConfig configuration,
            AeshContext aeshContext) {
        this.defaultValueProvider = defaultValueProvider;
        this.customizers = customizers;
        this.configuration = configuration;
        this.aeshContext = aeshContext;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AeshRuntimeRunner create() {
        Class<? extends Command> commandClass = resolveTopCommandClass();
        if (commandClass != null) {
            // Build settings with customizers applied so that custom providers
            // (e.g. CommandInvocationProvider) are available for runtime execution.
            var settings = CliSettingsHelper.createBaseSettings(configuration, customizers).build();

            // Pass the Class rather than a pre-constructed instance.
            // AeshRuntimeRunner defers construction to execute() when
            // lazyStartup is true (the default), so the command and its
            // subcommands are only instantiated when actually needed.
            // The AeshCdiCommandContainerBuilder handles CDI lookup
            // during materialization.
            AeshRuntimeRunner runner = AeshRuntimeRunner.builder()
                    .containerBuilder(new AeshCdiCommandContainerBuilder<>())
                    .command(commandClass);
            if (settings.commandInvocationProvider() != null) {
                runner.commandInvocationProvider(
                        (org.aesh.command.invocation.CommandInvocationProvider<?>) settings.commandInvocationProvider());
            }
            if (settings.converterInvocationProvider() != null) {
                runner.converterInvocationProvider(settings.converterInvocationProvider());
            }
            if (settings.validatorInvocationProvider() != null) {
                runner.validatorInvocationProvider(settings.validatorInvocationProvider());
            }
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
    private Class<? extends Command> resolveTopCommandClass() {
        // 1. Explicit config property overrides everything
        if (configuration.topCommand().isPresent()) {
            String topCommandName = configuration.topCommand().get();
            return loadCommandClass(topCommandName);
        }

        // 2. Use the build-time detected top command
        String topClassName = aeshContext.getTopCommandClassName();
        if (topClassName != null) {
            return loadCommandClass(topClassName);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Command> loadCommandClass(String className) {
        try {
            Class<?> commandClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            if (!Command.class.isAssignableFrom(commandClass)) {
                throw new IllegalStateException(
                        "Top command must implement org.aesh.command.Command interface: " + className);
            }
            if (commandClass.getAnnotation(CommandDefinition.class) == null) {
                throw new IllegalStateException(
                        "Top command must have a @CommandDefinition annotation: " + className);
            }
            return (Class<? extends Command>) commandClass;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to find or load top command: " + className, e);
        }
    }
}

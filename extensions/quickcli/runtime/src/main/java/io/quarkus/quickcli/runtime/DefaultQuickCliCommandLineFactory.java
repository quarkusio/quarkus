package io.quarkus.quickcli.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;

import io.quarkus.quickcli.CommandLine;
import io.quarkus.quickcli.runtime.annotations.TopCommand;

@ApplicationScoped
public class DefaultQuickCliCommandLineFactory implements QuickCliCommandLineFactory {

    private final Instance<Object> topCommand;
    private final QuickCliConfiguration configuration;
    private final CommandLine.Factory factory;

    public DefaultQuickCliCommandLineFactory(@TopCommand Instance<Object> topCommand,
            QuickCliConfiguration configuration,
            CommandLine.Factory factory) {
        this.topCommand = topCommand;
        this.configuration = configuration;
        this.factory = factory;
    }

    private Class<?> classForName(String name) {
        try {
            return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public CommandLine create() {
        if (configuration.topCommand().isPresent()) {
            String topCommandName = configuration.topCommand().get();

            Instance<Object> namedTopCommand = topCommand.select(NamedLiteral.of(topCommandName));
            if (namedTopCommand.isResolvable()) {
                return new CommandLine(namedTopCommand.get().getClass(), factory);
            }
            return new CommandLine(classForName(topCommandName), factory);
        }
        Object topCmd = topCommand.get();
        return new CommandLine(topCmd.getClass(), factory);
    }
}

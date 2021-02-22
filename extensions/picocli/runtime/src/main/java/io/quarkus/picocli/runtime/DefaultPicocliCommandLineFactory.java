package io.quarkus.picocli.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@ApplicationScoped
public class DefaultPicocliCommandLineFactory implements PicocliCommandLineFactory {

    private final Instance<Object> topCommand;
    private final PicocliConfiguration picocliConfiguration;
    private final CommandLine.IFactory picocliFactory;

    public DefaultPicocliCommandLineFactory(@TopCommand Instance<Object> topCommand,
            PicocliConfiguration picocliConfiguration,
            CommandLine.IFactory picocliFactory) {
        this.topCommand = topCommand;
        this.picocliConfiguration = picocliConfiguration;
        this.picocliFactory = picocliFactory;
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
        String topCommandName = picocliConfiguration.topCommand.orElse(null);
        if (topCommandName != null) {
            Instance<Object> namedTopCommand = topCommand.select(NamedLiteral.of(topCommandName));
            if (namedTopCommand.isResolvable()) {
                return new CommandLine(namedTopCommand.get(), picocliFactory);
            }
            return new CommandLine(classForName(topCommandName), picocliFactory);
        }
        return new CommandLine(topCommand.get(), picocliFactory);
    }
}

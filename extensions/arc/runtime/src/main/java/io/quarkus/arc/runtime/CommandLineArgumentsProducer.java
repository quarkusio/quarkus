package io.quarkus.arc.runtime;

import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.quarkus.runtime.annotations.CommandLineArguments;

@ApplicationScoped
public class CommandLineArgumentsProducer {

    private volatile Supplier<String[]> commandLineArgs;

    @Produces
    @CommandLineArguments
    public String[] getCommandLineArgs() {
        return commandLineArgs.get();
    }

    public void setCommandLineArgs(Supplier<String[]> commandLineArgs) {
        this.commandLineArgs = commandLineArgs;
    }
}

package io.quarkus.picocli.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.annotations.CommandLineArguments;
import picocli.CommandLine;

@ApplicationScoped
public class PicocliCommandLineProducer {

    @Produces
    @Singleton
    public CommandLine.IFactory picocliFactory() {
        return new PicocliBeansFactory();
    }

    @Produces
    @DefaultBean
    public CommandLine picocliCommandLine(PicocliCommandLineFactory picocliCommandLineFactory) {
        return picocliCommandLineFactory.create();
    }

    @Produces
    public CommandLine.ParseResult picocliParseResult(CommandLine commandLine, @CommandLineArguments String[] args) {
        return commandLine.parseArgs(args);
    }
}

package io.quarkus.picocli.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

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

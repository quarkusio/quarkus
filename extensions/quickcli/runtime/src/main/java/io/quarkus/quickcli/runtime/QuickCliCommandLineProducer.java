package io.quarkus.quickcli.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;
import io.quarkus.quickcli.CommandLine;
import io.quarkus.quickcli.ParseResult;
import io.quarkus.runtime.annotations.CommandLineArguments;

@ApplicationScoped
public class QuickCliCommandLineProducer {

    @Produces
    @Singleton
    public CommandLine.Factory quickCliFactory() {
        return new QuickCliBeansFactory();
    }

    @Produces
    @DefaultBean
    public CommandLine quickCliCommandLine(QuickCliCommandLineFactory commandLineFactory) {
        return commandLineFactory.create();
    }

    @Produces
    public ParseResult quickCliParseResult(CommandLine commandLine,
            @CommandLineArguments String[] args) {
        return commandLine.parse(args);
    }
}

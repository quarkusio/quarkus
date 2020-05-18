package io.quarkus.picocli.runtime;

import javax.enterprise.context.Dependent;

import io.quarkus.runtime.QuarkusApplication;
import picocli.CommandLine;

@Dependent
public class PicocliRunner implements QuarkusApplication {

    private final CommandLine commandLine;

    public PicocliRunner(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    @Override
    public int run(String... args) throws Exception {
        return commandLine.execute(args);
    }
}

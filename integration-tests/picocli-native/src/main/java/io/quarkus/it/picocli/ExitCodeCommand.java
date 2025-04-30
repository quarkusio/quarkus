package io.quarkus.it.picocli;

import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(name = "exitcode", versionProvider = DynamicVersionProvider.class)
public class ExitCodeCommand implements Callable<Integer> {

    @CommandLine.Option(names = "--code")
    int exitCode;

    @Override
    public Integer call() {
        return exitCode;
    }
}

package io.quarkus.quickcli.deployment;

import java.util.concurrent.Callable;

import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Option;

@Command(name = "exitcode", description = { "Returns a custom exit code" })
public class ExitCodeTestCommand implements Callable<Integer> {

    @Option(names = { "--code" }, description = "Exit code to return", defaultValue = "0")
    int exitCode;

    @Override
    public Integer call() {
        return exitCode;
    }
}

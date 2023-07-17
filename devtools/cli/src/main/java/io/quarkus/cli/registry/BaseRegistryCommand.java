package io.quarkus.cli.registry;

import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

public class BaseRegistryCommand implements Callable<Integer> {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected RegistryClientMixin registryClient;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Spec
    protected CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        spec.commandLine().usage(output.out());
        return CommandLine.ExitCode.OK;
    }
}

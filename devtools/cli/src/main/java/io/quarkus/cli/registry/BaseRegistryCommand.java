package io.quarkus.cli.registry;

import java.util.concurrent.Callable;

import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public class BaseRegistryCommand implements Callable<Integer> {

    @Mixin(name = "output")
    protected OutputOptionMixin output;

    @Mixin
    protected RegistryClientMixin registryClient;

    @Spec
    protected CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        spec.commandLine().usage(output.out());
        return CommandLine.ExitCode.OK;
    }
}

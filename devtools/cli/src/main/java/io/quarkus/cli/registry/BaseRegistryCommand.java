package io.quarkus.cli.registry;

import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.registry.RegistryClientMixin;
import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.ExitCode;
import io.quarkus.quickcli.annotations.Mixin;
import io.quarkus.quickcli.annotations.Spec;

public class BaseRegistryCommand implements Callable<Integer> {

    @Mixin(name = "output")
    protected OutputOptionMixin output;

    @Mixin
    protected RegistryClientMixin registryClient;

    @Mixin
    protected HelpOption helpOption;

    @Spec
    protected CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        spec.commandLine().usage(output.out());
        return ExitCode.OK;
    }
}

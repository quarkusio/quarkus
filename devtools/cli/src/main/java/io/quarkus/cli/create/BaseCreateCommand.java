package io.quarkus.cli.create;

import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.RunModeOption;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public class BaseCreateCommand implements Callable<Integer> {

    @Mixin
    protected RunModeOption runMode;

    @Mixin(name = "output")
    protected OutputOptionMixin output;

    @Mixin
    protected HelpOption helpOption;

    @Spec
    protected CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        spec.commandLine().usage(output.out());
        return CommandLine.ExitCode.OK;
    }
}

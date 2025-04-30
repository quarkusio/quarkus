package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.config.Encrypt;
import io.quarkus.cli.config.RemoveConfig;
import io.quarkus.cli.config.SetConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "config", header = "Manage Quarkus configuration", subcommands = { SetConfig.class, RemoveConfig.class,
        Encrypt.class })
public class Config implements Callable<Integer> {
    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @CommandLine.Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        spec.commandLine().usage(System.out);
        return 0;
    }
}

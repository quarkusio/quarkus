package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.config.Decrypt;
import io.quarkus.cli.config.Encrypt;
import io.quarkus.cli.config.RemoveConfig;
import io.quarkus.cli.config.SetConfig;
import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Mixin;
import io.quarkus.quickcli.annotations.Spec;
import io.quarkus.quickcli.annotations.Unmatched;

@Command(name = "config", header = "Manage Quarkus configuration", subcommands = { SetConfig.class, RemoveConfig.class,
        Encrypt.class, Decrypt.class })
public class Config implements Callable<Integer> {
    @Mixin(name = "output")
    protected OutputOptionMixin output;

    @Mixin
    protected HelpOption helpOption;

    @Spec
    protected CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        spec.commandLine().usage(output.out());
        return 0;
    }
}

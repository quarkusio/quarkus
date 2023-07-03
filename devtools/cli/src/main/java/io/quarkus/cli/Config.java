package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.config.Encrypt;
import io.quarkus.cli.config.SetConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "config", header = "Manage Quarkus configuration", subcommands = { SetConfig.class, Encrypt.class })
public class Config implements Callable<Integer> {
    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @CommandLine.Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        CommandLine.ParseResult result = spec.commandLine().getParseResult();
        CommandLine appCommand = spec.subcommands().get("set");
        return appCommand.execute(result.originalArgs().stream().filter(x -> !"config".equals(x)).toArray(String[]::new));
    }
}

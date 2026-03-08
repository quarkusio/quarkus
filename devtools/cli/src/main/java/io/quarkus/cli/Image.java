package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.image.Build;
import io.quarkus.cli.image.Push;
import io.quarkus.quickcli.CommandLine;
import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.ParseResult;
import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Mixin;
import io.quarkus.quickcli.annotations.Spec;
import io.quarkus.quickcli.annotations.Unmatched;

@Command(name = "image", sortOptions = false, mixinStandardHelpOptions = false, header = "Build or push project container image.", subcommands = {
        Build.class, Push.class
}, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class Image implements Callable<Integer> {

    @Mixin(name = "output")
    protected OutputOptionMixin output;

    @Mixin
    protected HelpOption helpOption;

    @Spec
    protected CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    public Integer call() throws Exception {
        ParseResult result = spec.commandLine().getParseResult();
        CommandLine buildCommand = spec.commandLine().getSubcommands().get("build");
        return buildCommand.execute(result.originalArgs().stream().filter(x -> !"image".equals(x)).toArray(String[]::new));
    }
}

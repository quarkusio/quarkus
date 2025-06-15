package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.image.Build;
import io.quarkus.cli.image.Push;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Unmatched;

@CommandLine.Command(name = "image", sortOptions = false, mixinStandardHelpOptions = false, header = "Build or push project container image.", subcommands = {
        Build.class,
        Push.class }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class Image implements Callable<Integer> {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    public Integer call() throws Exception {
        ParseResult result = spec.commandLine().getParseResult();
        CommandLine buildCommand = spec.subcommands().get("build");
        return buildCommand
                .execute(result.originalArgs().stream().filter(x -> !"image".equals(x)).toArray(String[]::new));
    }
}

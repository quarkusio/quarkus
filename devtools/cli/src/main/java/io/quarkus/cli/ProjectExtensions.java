package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.quickcli.CommandLine;
import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.ParseResult;
import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Mixin;
import io.quarkus.quickcli.annotations.Spec;
import io.quarkus.quickcli.annotations.Unmatched;

@Command(name = "extension", aliases = {
        "ext" }, header = "Configure extensions of an existing project.", subcommands = {
                ProjectExtensionsList.class,
                ProjectExtensionsCategories.class,
                ProjectExtensionsAdd.class,
                ProjectExtensionsRemove.class })
public class ProjectExtensions implements Callable<Integer> {

    @Mixin(name = "output")
    protected OutputOptionMixin output;

    @Spec
    protected CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        output.info("Listing extensions (default action, see --help).");

        ParseResult result = spec.commandLine().getParseResult();
        List<String> args = result.originalArgs().stream().filter(x -> !"extension".equals(x) && !"ext".equals(x))
                .collect(Collectors.toList());
        CommandLine listCommand = spec.commandLine().getSubcommands().get("list");
        return listCommand.execute(args.toArray(new String[0]));
    }
}

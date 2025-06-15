package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.plugin.CliPluginsAdd;
import io.quarkus.cli.plugin.CliPluginsList;
import io.quarkus.cli.plugin.CliPluginsRemove;
import io.quarkus.cli.plugin.CliPluginsSync;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Unmatched;

@CommandLine.Command(name = "plugin", aliases = {
        "plug" }, header = "Configure plugins of the Quarkus CLI.", subcommands = { CliPluginsList.class,
                CliPluginsAdd.class, CliPluginsRemove.class, CliPluginsSync.class })
public class CliPlugins implements Callable<Integer> {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        output.info("Listing plugins (default action, see --help).");
        ParseResult result = spec.commandLine().getParseResult();
        List<String> args = result.originalArgs().stream().filter(x -> !"plugin".equals(x) && !"plug".equals(x))
                .collect(Collectors.toList());
        CommandLine listCommand = spec.subcommands().get("list");
        return listCommand.execute(args.toArray(new String[0]));
    }
}

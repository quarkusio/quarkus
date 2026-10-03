package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.plugin.CliPluginsAdd;
import io.quarkus.cli.plugin.CliPluginsList;
import io.quarkus.cli.plugin.CliPluginsRemove;
import io.quarkus.cli.plugin.CliPluginsSync;
import picocli.CommandLine;
import picocli.CommandLine.Unmatched;

@CommandLine.Command(name = "plugin", aliases = { "plug" }, header = "Configure plugins of the Quarkus CLI.", subcommands = {
        CliPluginsList.class,
        CliPluginsAdd.class,
        CliPluginsRemove.class,
        CliPluginsSync.class
})
public class CliPlugins implements Callable<Integer> {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        return DefaultSubcommand.forward(spec, output, "list", "Listing plugins (default action, see --help).");
    }
}

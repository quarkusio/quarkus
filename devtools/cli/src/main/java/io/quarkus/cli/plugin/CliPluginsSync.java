package io.quarkus.cli.plugin;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.RunModeOption;
import picocli.CommandLine;

@CommandLine.Command(name = "sync", header = "Sync (discover / purge) CLI Plugins.")
public class CliPluginsSync extends CliPluginsBase implements Callable<Integer> {

    @CommandLine.Mixin
    RunModeOption runMode;

    PluginCatalogService pluginCatalogService = new PluginCatalogService();

    @Override
    public Integer call() {
        output.throwIfUnmatchedArguments(spec.commandLine());

        if (runMode.isDryRun()) {
            dryRunAdd(spec.commandLine().getHelp());
            return CommandLine.ExitCode.OK;
        }

        pluginManager().sync();
        return CommandLine.ExitCode.OK;
    }

    void dryRunAdd(CommandLine.Help help) {
        output.printText(new String[] {
                "\tSync plugin to the CLI\n",
                "\t" + projectRoot().toString()
        });
        Map<String, String> dryRunOutput = new TreeMap<>();
        output.info(help.createTextTable(dryRunOutput).toString());
    };
}

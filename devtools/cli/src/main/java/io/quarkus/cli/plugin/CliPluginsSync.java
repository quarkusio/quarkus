package io.quarkus.cli.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import io.quarkus.cli.common.RunModeOption;
import picocli.CommandLine;

@CommandLine.Command(name = "sync", header = "Sync (discover / purge) CLI Plugins.")
public class CliPluginsSync extends CliPluginsBase implements Callable<Integer> {

    @CommandLine.Mixin
    RunModeOption runMode;

    @Override
    public Integer call() {
        output.throwIfUnmatchedArguments(spec.commandLine());

        if (runMode.isDryRun()) {
            dryRunAdd(spec.commandLine().getHelp());
            return CommandLine.ExitCode.OK;
        }

        PluginManager pluginManager = pluginManager();
        Map<String, Plugin> before = pluginManager.getInstalledPlugins();
        if (pluginManager.sync()) {
            Map<String, Plugin> after = pluginManager.getInstalledPlugins();

            Map<String, PluginListItem> installed = after.entrySet().stream().filter(e -> !before.containsKey(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> new PluginListItem(true, e.getValue())));
            Map<String, PluginListItem> uninstalled = before.entrySet().stream().filter(e -> !after.containsKey(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> new PluginListItem(false, e.getValue())));
            Map<String, PluginListItem> all = new HashMap<>();
            all.putAll(installed);
            all.putAll(uninstalled);

            PluginListTable table = new PluginListTable(all.values(), false, true);
            output.info("Sync completed. The following plugins were added/removed:");
            output.info(table.getContent());
        } else {
            output.info("Nothing to sync (no plugins were added or removed).");
        }
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

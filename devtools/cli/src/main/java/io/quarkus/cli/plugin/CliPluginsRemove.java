package io.quarkus.cli.plugin;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.RunModeOption;
import picocli.CommandLine;

@CommandLine.Command(name = "remove", header = "Remove plugin(s) to the Quarkus CLI.")
public class CliPluginsRemove extends CliPluginsBase implements Callable<Integer> {

    @CommandLine.Mixin
    RunModeOption runMode;

    @CommandLine.Parameters(arity = "1", paramLabel = "PLUGIN_NAME", description = "Plugin name to remove from the CLI")
    String name;

    @Override
    public Integer call() {
        try {
            output.debug("Remove plugin with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            if (runMode.isDryRun()) {
                dryRunRemove(spec.commandLine().getHelp());
                return CommandLine.ExitCode.OK;
            }

            return removePlugin();
        } catch (Exception e) {
            return output.handleCommandException(e, "Unable to remove extension(s): " + e.getMessage());
        }
    }

    Integer removePlugin() throws IOException {
        PluginManager pluginManager = pluginManager();
        Optional<Plugin> removedPlugin = pluginManager.removePlugin(name, catalogOptions.user);

        return removedPlugin.map(plugin -> {
            PluginListTable table = new PluginListTable(List.of(new PluginListItem(false, plugin)), false);
            output.info("Removed plugin:");
            output.info(table.getContent());
            // Check if plugin still exists
            if (pluginManager.getInstalledPlugins().containsKey(plugin.getName())) {
                if (plugin.isInProjectCatalog()) {
                    output.warn(
                            "The removed plugin was available both in user and project scopes. It was removed from the project but will remain available in the user scope!");
                } else {
                    output.warn(
                            "The removed plugin was available both in user and project scopes. It was removed from the user but will remain available in the project scope!");
                }
            }
            return CommandLine.ExitCode.OK;
        }).orElseGet(() -> {
            output.error("Plugin: " + name + " not found in catalog!");
            return CommandLine.ExitCode.USAGE;
        });
    }

    void dryRunRemove(CommandLine.Help help) {
        output.printText(new String[] { "\nRemove plugin from the CLI\n", "\t" + projectRoot().toString() });
        Map<String, String> dryRunOutput = new TreeMap<>();
        dryRunOutput.put("Plugin to remove", name);
        output.info(help.createTextTable(dryRunOutput).toString());
    };

}

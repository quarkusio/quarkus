package io.quarkus.cli.plugin;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.RunModeOption;
import picocli.CommandLine;

@CommandLine.Command(name = "add", header = "Add plugin(s) to the Quarkus CLI.")
public class CliPluginsAdd extends CliPluginsBase implements Callable<Integer> {

    @CommandLine.Mixin
    RunModeOption runMode;

    @CommandLine.Option(names = { "-d",
            "--description" }, paramLabel = "Plugin description", order = 5, description = "The plugin description")
    Optional<String> description;

    @CommandLine.Parameters(arity = "1", paramLabel = "PLUGIN_NAME", description = " The plugin name or location (e.g. url, path or maven coordinates in GACTV form)")
    String nameOrLocation;

    @Override
    public Integer call() {
        try {
            output.debug("Add plugin with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            if (runMode.isDryRun()) {
                dryRunAdd(spec.commandLine().getHelp());
                return CommandLine.ExitCode.OK;
            }

            return addPlugin();
        } catch (Exception e) {
            return output.handleCommandException(e, "Unable to add plugin(s): " + nameOrLocation + " of type: "
                    + type.map(PluginType::name).orElse("<any>") + "." + e.getMessage());
        }
    }

    Integer addPlugin() throws IOException {
        PluginManager pluginManager = pluginManager();
        String name = pluginManager.getUtil().getName(nameOrLocation);
        Optional<Plugin> existingPlugin = Optional.ofNullable(pluginManager.getInstalledPlugins().get(name));
        Optional<Plugin> addedPlugin = pluginManager.addPlugin(nameOrLocation, catalogOptions.user, description);

        return addedPlugin.map(plugin -> {
            PluginListTable table = new PluginListTable(List.of(new PluginListItem(true, plugin)), false);
            output.info("Added plugin:");
            output.info(table.getContent());
            if (plugin.isInProjectCatalog() && existingPlugin.filter(p -> p.isInUserCatalog()).isPresent()) {
                output.warn(
                        "Plugin was added in the project scope, but another with the same name exists in the user scope!\nThe project scoped one will take precedence when invoked from within the project!");
            }

            if (plugin.isInUserCatalog() && existingPlugin.filter(p -> p.isInProjectCatalog()).isPresent()) {
                output.warn(
                        "Plugin was added in the user scope, but another with the same name exists in the project scope!\nThe project scoped one will take precedence when invoked from within the project!");
            }

            return CommandLine.ExitCode.OK;
        }).orElseGet(() -> {
            output.error("No plugin available at: " + this.nameOrLocation);
            printHints(true);
            return CommandLine.ExitCode.USAGE;
        });
    }

    private void printHints(boolean pluginListHint) {
        if (runMode.isBatchMode())
            return;

        if (pluginListHint) {
            output.info("To see the list of installable plugins, use the 'plugin list' subcommand.");
        }
    }

    void dryRunAdd(CommandLine.Help help) {
        output.printText(new String[] { "\nAdd plugin to the CLI\n", "\t" + projectRoot().toString() });
        Map<String, String> dryRunOutput = new TreeMap<>();
        dryRunOutput.put("Name or Location", nameOrLocation);
        type.ifPresent(t -> dryRunOutput.put("Type", t.name()));
        output.info(help.createTextTable(dryRunOutput).toString());
    };
}

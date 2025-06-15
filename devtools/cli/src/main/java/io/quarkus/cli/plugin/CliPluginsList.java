package io.quarkus.cli.plugin;

import static io.quarkus.devtools.utils.Patterns.isExpression;
import static io.quarkus.devtools.utils.Patterns.toRegex;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.cli.common.RunModeOption;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.runtime.util.StringUtil;
import picocli.CommandLine;

@CommandLine.Command(name = "list", aliases = "ls", header = "List CLI plugins. ")
public class CliPluginsList extends CliPluginsBase implements Callable<Integer> {

    @CommandLine.Mixin
    RunModeOption runMode;

    @CommandLine.Option(names = { "-i",
            "--installable" }, defaultValue = "false", order = 4, description = "List plugins that can be installed")
    boolean installable = false;

    @CommandLine.Option(names = { "-s",
            "--search" }, defaultValue = "*", order = 5, paramLabel = "PATTERN", description = "Search for matching plugins (simple glob using '*' and '?').")
    String searchPattern;

    @CommandLine.Option(names = { "-c",
            "--show-command" }, defaultValue = "false", order = 6, description = "Show the command that corresponds to the plugin")
    boolean showCommand = false;

    Map<String, Plugin> installedPlugins = new HashMap<>();

    @Override
    public Integer call() {
        try {
            output.debug("List extensions with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            if (runMode.isDryRun()) {
                return dryRunList(spec.commandLine().getHelp(), null);
            }
            Integer exitCode = listPluigns();
            printHints(!installable && installedPlugins.isEmpty(), installable);
            return exitCode;
        } catch (Exception e) {
            return output.handleCommandException(e, "Unable to list plugins: " + e.getMessage());
        }
    }

    Integer dryRunList(CommandLine.Help help, BuildTool buildTool) {
        Map<String, String> dryRunOutput = new TreeMap<>();
        output.printText(new String[] { "\nList plugins\n" });
        dryRunOutput.put("Search pattern", searchPattern);
        dryRunOutput.put("List installable", String.valueOf(installable));
        dryRunOutput.put("Type", String.valueOf(type));
        dryRunOutput.put("Only user", String.valueOf(catalogOptions.user));

        output.info(help.createTextTable(dryRunOutput).toString());
        return CommandLine.ExitCode.OK;
    }

    Integer listPluigns() {
        PluginManager pluginManager = pluginManager();
        Predicate<Plugin> pluginFilter = pluginFilter();
        pluginManager.reconcile();
        installedPlugins.putAll(pluginManager.getInstalledPlugins(catalogOptions.user));

        Map<String, PluginListItem> items = new HashMap<>();
        if (installable) {
            Map<String, Plugin> availablePlugins = pluginManager.getInstallablePlugins();
            items.putAll(availablePlugins.values().stream().filter(p -> !installedPlugins.containsKey(p.getName()))
                    .filter(pluginFilter).map(p -> new PluginListItem(installedPlugins.containsKey(p.getName()), p))
                    .collect(Collectors.toMap(p -> p.getName(), p -> p)));
        }

        items.putAll(installedPlugins.entrySet().stream().filter(e -> pluginFilter.test(e.getValue()))
                .map(e -> new PluginListItem(true, e.getValue())).collect(Collectors.toMap(p -> p.getName(), p -> p)));

        if (items.isEmpty()) {
            output.info("No plugins " + (installable ? "installable" : "installed") + "!");
        } else {
            PluginListTable table = new PluginListTable(
                    items.values().stream().filter(this::filter).collect(Collectors.toList()), showCommand);
            output.info(table.getContent());
        }
        return CommandLine.ExitCode.OK;
    }

    private void printHints(boolean installableHint, boolean remoteHint) {
        if (runMode.isBatchMode())
            return;

        if (installableHint) {
            output.info("To include the installable plugins in the list, append --installable to the command.");
        }

        if (remoteHint) {
            output.info(
                    "Use the 'plugin add' subcommand and pass the location of any plugin listed above, or any remote location in the form of URL / GACTV pointing to a remote plugin.");
        }
    }

    private boolean filter(PluginListItem item) {
        if (StringUtil.isNullOrEmpty(searchPattern)) {
            return true;
        }
        if (!isExpression(searchPattern)) {
            return item.getName().contains(searchPattern);
        }
        Pattern p = toRegex(searchPattern);
        return p.matcher(item.getName()).matches();
    }

    @Override
    public String toString() {
        return "CliPluginsList [" + ", output=" + output + ", runMode=" + runMode + "]";
    }
}

package io.quarkus.cli;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.cli.common.ListFormatOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;

@CommandLine.Command(name = "list", aliases = "ls", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, description = "List platforms and extensions for this project.")
public class ProjectExtensionsList extends BaseBuildCommand implements Callable<Integer> {

    @Mixin
    RunModeOption runMode;

    @CommandLine.Option(names = { "-i",
            "--installable" }, defaultValue = "false", order = 2, description = "Display installable extensions.")
    boolean installable = false;

    @CommandLine.Option(names = { "-s",
            "--search" }, defaultValue = "*", paramLabel = "PATTERN", order = 3, description = "Search filter on extension list. The format is based on Java Pattern.")
    String searchPattern;

    @CommandLine.ArgGroup(heading = "%nOutput format%n")
    ListFormatOptions format = new ListFormatOptions();

    @Override
    public Integer call() {
        try {
            output.debug("List extensions with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            BuildSystemRunner runner = getRunner();
            if (runMode.isDryRun()) {
                dryRunList(spec.commandLine().getHelp(), runner.getBuildTool());
                return CommandLine.ExitCode.OK;
            }

            return runner.listExtensions(runMode, format, installable, searchPattern);
        } catch (Exception e) {
            return output.handleCommandException(e,
                    "Unable to list extensions: " + e.getMessage());
        }
    }

    void dryRunList(CommandLine.Help help, BuildTool buildTool) {
        output.printText(new String[] {
                "\nList extensions for current project\n",
                "\t" + projectRoot().toString()
        });
        Map<String, String> dryRunOutput = new TreeMap<>();

        dryRunOutput.put("Build tool", buildTool.name());
        dryRunOutput.put("Batch (non-interactive mode)", Boolean.toString(runMode.isBatchMode()));
        dryRunOutput.put("List format", format.getFormatString());
        dryRunOutput.put("List installable extensions", Boolean.toString(installable));
        dryRunOutput.put("Search pattern", searchPattern);

        output.info(help.createTextTable(dryRunOutput).toString());
    }

    @Override
    public String toString() {
        return "ProjectExtensionList [format=" + format
                + ", installable=" + installable
                + ", searchPattern=" + searchPattern
                + ", output=" + output
                + ", runMode=" + runMode
                + "]";
    }
}

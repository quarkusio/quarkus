package io.quarkus.cli;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;

@CommandLine.Command(name = "remove", aliases = "rm", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Remove extension(s) from this project.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "%nOptions:%n")
public class ProjectExtensionsRemove extends BaseBuildCommand implements Callable<Integer> {

    @Mixin
    RunModeOption runMode;

    @CommandLine.Parameters(arity = "1", paramLabel = "EXTENSION", description = "Extension(s) to remove from this project.")
    Set<String> extensions;

    @Override
    public Integer call() {
        try {
            output.debug("Remove extensions with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            BuildSystemRunner runner = getRunner();
            if (runMode.isDryRun()) {
                dryRunRemove(spec.commandLine().getHelp(), runner.getBuildTool());
                return CommandLine.ExitCode.OK;
            }

            return runner.removeExtension(runMode, extensions);
        } catch (Exception e) {
            return output.handleCommandException(e,
                    "Unable to remove extension(s): " + e.getMessage());
        }
    }

    void dryRunRemove(CommandLine.Help help, BuildTool buildTool) {
        output.printText(new String[] {
                "\nRemove extensions to the current project in \n",
                "\t" + projectRoot().toString()
        });
        Map<String, String> dryRunOutput = new TreeMap<>();

        dryRunOutput.put("Build tool", buildTool.name());
        dryRunOutput.put("Batch (non-interactive mode)", Boolean.toString(runMode.isBatchMode()));
        dryRunOutput.put("Extension(s) to remove", extensions.toString());

        output.info(help.createTextTable(dryRunOutput).toString());
    };

    @Override
    public String toString() {
        return "ProjectExtensionRemove [extensions=" + extensions + "]";
    }
}

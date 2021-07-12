package io.quarkus.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@CommandLine.Command(name = "build", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, showEndOfOptionsDelimiterInUsageHelp = true, header = "Build the current project.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Build extends BaseBuildCommand implements Callable<Integer> {

    @CommandLine.Mixin
    protected RunModeOption runMode;

    @CommandLine.ArgGroup(order = 1, exclusive = false, validate = false, heading = "%nBuild options:%n")
    BuildOptions buildOptions = new BuildOptions();

    @Parameters(description = "Additional parameters passed to the build system")
    List<String> params = new ArrayList<>();

    @Override
    public Integer call() {
        try {
            output.debug("Build project with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            BuildSystemRunner runner = getRunner();
            BuildSystemRunner.BuildCommandArgs commandArgs = runner.prepareBuild(buildOptions, runMode, params);

            if (runMode.isDryRun()) {
                dryRunBuild(spec.commandLine().getHelp(), runner.getBuildTool(), commandArgs);
                return CommandLine.ExitCode.OK;
            }

            return runner.run(commandArgs);
        } catch (Exception e) {
            return output.handleCommandException(e,
                    "Unable to build project: " + e.getMessage());
        }
    }

    void dryRunBuild(CommandLine.Help help, BuildTool buildTool, BuildSystemRunner.BuildCommandArgs args) {
        output.printText(new String[] {
                "\nBuild current project\n",
                "\t" + projectRoot().toString()
        });
        Map<String, String> dryRunOutput = new TreeMap<>();
        dryRunOutput.put("Build tool", buildTool.name());
        output.info(help.createTextTable(dryRunOutput).toString());

        output.printText(new String[] {
                "\nCommand line:\n",
                args.showCommand()
        });
    }

    @Override
    public String toString() {
        return "Build [buildOptions=" + buildOptions
                + ", properties=" + propertiesOptions.properties
                + ", output=" + output
                + ", params=" + params + "]";
    }
}

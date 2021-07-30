package io.quarkus.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.cli.common.DebugOptions;
import io.quarkus.cli.common.DevOptions;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@CommandLine.Command(name = "dev", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, showEndOfOptionsDelimiterInUsageHelp = true, header = "Run the current project in dev (live coding) mode.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Dev extends BaseBuildCommand implements Callable<Integer> {

    @CommandLine.ArgGroup(order = 1, exclusive = false, heading = "%nDev Mode options:%n")
    DevOptions devOptions = new DevOptions();

    @CommandLine.ArgGroup(order = 3, exclusive = false, validate = true, heading = "%nDebug options:%n")
    DebugOptions debugOptions = new DebugOptions();

    @Parameters(description = "Parameters passed to the application.")
    List<String> params = new ArrayList<>();

    @Override
    public Integer call() {
        try {
            output.debug("Run project in dev mode with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            BuildSystemRunner runner = getRunner();
            List<Supplier<BuildSystemRunner.BuildCommandArgs>> commandArgs = runner.prepareDevMode(devOptions, debugOptions,
                    params);

            if (devOptions.isDryRun()) {
                dryRunDev(spec.commandLine().getHelp(), runner.getBuildTool(), commandArgs.iterator().next().get());
                return CommandLine.ExitCode.OK;
            }
            Integer ret = 1;
            for (Supplier<BuildSystemRunner.BuildCommandArgs> i : commandArgs) {
                ret = runner.run(i.get());
                if (ret != 0) {
                    return ret;
                }
            }
            return ret;
        } catch (Exception e) {
            return output.handleCommandException(e,
                    "Unable to launch project in dev mode: " + e.getMessage());
        }
    }

    void dryRunDev(CommandLine.Help help, BuildTool buildTool, BuildSystemRunner.BuildCommandArgs args) {
        output.printText(new String[] {
                "\nRun current project in dev mode\n",
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
        return "Dev [debugOptions=" + debugOptions
                + ", devOptions=" + devOptions
                + ", properties=" + propertiesOptions.properties
                + ", output=" + output
                + ", params=" + params + "]";
    }
}

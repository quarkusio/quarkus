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

@CommandLine.Command(name = "test", showEndOfOptionsDelimiterInUsageHelp = true, header = "Run the current project in continuous testing mode.")
public class Test extends BaseBuildCommand implements Callable<Integer> {

    @CommandLine.ArgGroup(order = 1, exclusive = false, heading = "%nContinuous Test Mode options:%n")
    DevOptions testOptions = new DevOptions();

    @CommandLine.ArgGroup(order = 3, exclusive = false, validate = true, heading = "%nDebug options:%n")
    DebugOptions debugOptions = new DebugOptions();

    @Parameters(description = "Parameters passed to the application.")
    List<String> params = new ArrayList<>();

    @Override
    public Integer call() {
        try {
            output.debug("Run project in test mode with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            BuildSystemRunner runner = getRunner();
            List<Supplier<BuildSystemRunner.BuildCommandArgs>> commandArgs = runner.prepareDevTestMode(
                    false, testOptions, debugOptions, params);

            if (testOptions.isDryRun()) {
                dryRunTest(spec.commandLine().getHelp(), runner.getBuildTool(), commandArgs.iterator().next().get());
                return CommandLine.ExitCode.OK;
            }
            int ret = 1;
            for (Supplier<BuildSystemRunner.BuildCommandArgs> i : commandArgs) {
                ret = runner.run(i.get());
                if (ret != 0) {
                    return ret;
                }
            }
            return ret;
        } catch (Exception e) {
            return output.handleCommandException(e,
                    "Unable to launch project in test mode: " + e.getMessage());
        }
    }

    void dryRunTest(CommandLine.Help help, BuildTool buildTool, BuildSystemRunner.BuildCommandArgs args) {
        output.printText("\nRun current project in test mode\n",
                "\t" + projectRoot().toString());
        Map<String, String> dryRunOutput = new TreeMap<>();
        dryRunOutput.put("Build tool", buildTool.name());
        output.info(help.createTextTable(dryRunOutput).toString());

        output.printText("\nCommand line:\n",
                args.showCommand());
    }

    @Override
    public String toString() {
        return "Test [debugOptions=" + debugOptions
                + ", testOptions=" + testOptions
                + ", properties=" + propertiesOptions.properties
                + ", output=" + output
                + ", params=" + params + "]";
    }
}

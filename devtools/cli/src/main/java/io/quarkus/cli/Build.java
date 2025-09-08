package io.quarkus.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.cli.registry.ToggleRegistryClientMixin;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;

@CommandLine.Command(name = "build", showEndOfOptionsDelimiterInUsageHelp = true, header = "Build the current project.")
public class Build extends BaseBuildCommand implements Callable<Integer> {

    @CommandLine.Mixin
    protected RunModeOption runMode;

    @CommandLine.ArgGroup(order = 1, exclusive = false, validate = false, heading = "%nBuild options:%n")
    BuildOptions buildOptions = new BuildOptions();

    @Parameters(description = "Additional parameters passed to the build system")
    List<String> params = new ArrayList<>();

    public Build() {
        super();
    }

    public Build(final RunModeOption runMode, final BuildOptions buildOptions, final List<String> params,
            final CommandSpec spec,
            final OutputOptionMixin output, final ToggleRegistryClientMixin registryClient, final HelpOption helpOption,
            final PropertiesOptions propertiesOptions) {
        super(spec, output, registryClient, helpOption, propertiesOptions);
        this.runMode = runMode;
        this.buildOptions = buildOptions;
        this.params = params;
    }

    @Override
    public Integer call() {
        try {
            output.debug("Build project with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            BuildSystemRunner runner = getRunner();
            if (buildOptions.generateReport) {
                params.add("-Dquarkus.debug.dump-build-metrics=true");
            }
            BuildSystemRunner.BuildCommandArgs commandArgs = runner.prepareBuild(buildOptions, runMode, params);

            if (runMode.isDryRun()) {
                dryRunBuild(spec.commandLine().getHelp(), runner.getBuildTool(), commandArgs);
                return CommandLine.ExitCode.OK;
            }

            int exitCode = runner.run(commandArgs);
            if (exitCode == CommandLine.ExitCode.OK && buildOptions.generateReport) {
                output.printText(new String[] {
                        "\nBuild report available: " + new BuildReport(runner).generate().toPath().toAbsolutePath().toString()
                                + "\n"
                });
            }
            return exitCode;
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

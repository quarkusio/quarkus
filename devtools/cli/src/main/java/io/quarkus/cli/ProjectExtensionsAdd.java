package io.quarkus.cli;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.RunModeOption;
import io.quarkus.cli.common.build.BuildSystemRunner;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.quickcli.ExitCode;
import io.quarkus.quickcli.Help;
import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Mixin;
import io.quarkus.quickcli.annotations.Parameters;

@Command(name = "add", header = "Add extension(s) to this project.")
public class ProjectExtensionsAdd extends BaseBuildCommand implements Callable<Integer> {

    @Mixin
    RunModeOption runMode;

    @Parameters(arity = "1", paramLabel = "EXTENSION", description = "extensions to add to this project", split = ",")
    Set<String> extensions;

    @Override
    public Integer call() {
        try {
            output.debug("Add extensions with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            BuildSystemRunner runner = getRunner();
            if (runMode.isDryRun()) {
                dryRunAdd(spec.commandLine().getHelp(), runner.getBuildTool());
                return ExitCode.OK;
            }

            return runner.addExtension(runMode, extensions);
        } catch (Exception e) {
            return output.handleCommandException(e,
                    "Unable to add extension(s): " + e.getMessage());
        }
    }

    void dryRunAdd(Help help, BuildTool buildTool) {
        output.printText(new String[] {
                "\nAdd extensions to the current project\n",
                "\t" + projectRoot().toString()
        });
        Map<String, String> dryRunOutput = new TreeMap<>();

        dryRunOutput.put("Build tool", buildTool.name());
        dryRunOutput.put("Batch (non-interactive mode)", Boolean.toString(runMode.isBatchMode()));
        dryRunOutput.put("Extension(s) to add", extensions.toString());

        output.info(help.createTextTable(dryRunOutput).toString());
    };
}

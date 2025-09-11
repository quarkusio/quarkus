package io.quarkus.cli;

import java.util.concurrent.Callable;

import io.quarkus.cli.common.build.BuildSystemRunner;
import picocli.CommandLine;

@CommandLine.Command(name = "info", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Display project information and verify versions health (platform and extensions).", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "%nOptions:%n")
public class Info extends BaseBuildCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "--per-module" }, description = "Display information per project module.")
    public boolean perModule = false;

    @Override
    public Integer call() throws Exception {
        try {
            final BuildSystemRunner runner = getRunner();
            return runner.projectInfo(perModule);
        } catch (Exception e) {
            return output.handleCommandException(e, "Unable to collect Quarkus project information: " + e.getMessage());
        }
    }
}

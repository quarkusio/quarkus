package io.quarkus.cli;

import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.build.BuildSystemRunner;
import picocli.CommandLine;

@CommandLine.Command(name = "update", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Display recommended Quarkus updates.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "%nOptions:%n")
public class Update extends BaseBuildCommand implements Callable<Integer> {

    @CommandLine.Option(names = {
            "--rectify" }, description = "Display platform and/or extension version alignment recommendations.")
    public boolean rectify = false;

    @CommandLine.Option(names = {
            "--recommended-state" }, description = "Display the state of the project as if the recommended updates were applied.")
    public boolean recommendedState = false;

    @CommandLine.Option(names = { "--per-module" }, description = "Display information per project module.")
    public boolean perModule = false;

    @Override
    public Integer call() throws Exception {
        try {
            final BuildSystemRunner runner = getRunner();
            return runner.update(rectify, recommendedState, perModule);
        } catch (Exception e) {
            return output.handleCommandException(e, "Unable to collect Quarkus project information: " + e.getMessage());
        }
    }
}

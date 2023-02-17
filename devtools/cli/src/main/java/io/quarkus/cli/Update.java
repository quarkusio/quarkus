package io.quarkus.cli;

import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import picocli.CommandLine;

@CommandLine.Command(name = "update", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Suggest recommended project updates with the possibility to apply them.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "%nOptions:%n")
public class Update extends BaseBuildCommand implements Callable<Integer> {

    @CommandLine.ArgGroup(order = 0, heading = "%nTarget Quarkus version:%n", multiplicity = "0..1")
    TargetQuarkusVersionGroup targetQuarkusVersion = new TargetQuarkusVersionGroup();

    @CommandLine.Option(order = 1, names = { "--per-module" }, description = "Display information per project module.")
    public boolean perModule = false;

    @Override
    public Integer call() throws Exception {
        try {
            final BuildSystemRunner runner = getRunner();
            return runner.updateProject(targetQuarkusVersion.platformVersion, targetQuarkusVersion.streamId, perModule);
        } catch (Exception e) {
            return output.handleCommandException(e, "Unable to collect Quarkus project information: " + e.getMessage());
        }
    }
}

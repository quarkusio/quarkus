package io.quarkus.cli;

import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.cli.update.RewriteGroup;
import picocli.CommandLine;

@CommandLine.Command(name = "update", aliases = { "up",
        "upgrade" }, sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Suggest project updates and create a recipe with the possibility to apply it.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "%nOptions:%n")
public class Update extends BaseBuildCommand implements Callable<Integer> {

    @CommandLine.ArgGroup(order = 0, heading = "%nTarget Quarkus version:%n", multiplicity = "0..1")
    TargetQuarkusVersionGroup targetQuarkusVersion = new TargetQuarkusVersionGroup();

    @CommandLine.ArgGroup(order = 1, heading = "%nRewrite (interactive by default):%n", exclusive = false)
    RewriteGroup rewrite = new RewriteGroup();

    @Override
    public Integer call() throws Exception {
        try {
            final BuildSystemRunner runner = getRunner();
            return runner.updateProject(targetQuarkusVersion, rewrite);
        } catch (Exception e) {
            return output.handleCommandException(e, "Unable to run Quarkus project update : " + e.getMessage());
        }
    }
}

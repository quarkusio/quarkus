package io.quarkus.cli;

import java.nio.file.Path;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.cli.core.BuildsystemCommand;
import io.quarkus.devtools.commands.ListPlatforms;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "platforms", sortOptions = false, mixinStandardHelpOptions = false, description = "List imported (default) or all available Quarkus platforms.")
public class Platforms extends BaseSubCommand implements BuildsystemCommand {

    @Override
    public int execute(Path projectDirectory, BuildTool buildTool) {
        try {
            new ListPlatforms(
                    QuarkusCliUtils.getQuarkusProject(buildTool == null ? BuildTool.MAVEN : buildTool, projectDirectory))
                            .execute();
        } catch (Exception e) {
            if (parent.showErrors) {
                e.printStackTrace(err());
            }
            return CommandLine.ExitCode.SOFTWARE;
        }
        return CommandLine.ExitCode.OK;
    }

}

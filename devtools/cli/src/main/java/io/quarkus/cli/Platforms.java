package io.quarkus.cli;

import java.nio.file.Path;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.cli.core.BuildsystemCommand;
import io.quarkus.cli.core.QuarkusCliVersion;
import io.quarkus.devtools.commands.ListPlatforms;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import picocli.CommandLine;

@CommandLine.Command(name = "platforms", usageHelpAutoWidth = true, sortOptions = false, mixinStandardHelpOptions = false, description = "List imported (default) or all available Quarkus platforms.")
public class Platforms extends BaseSubCommand implements BuildsystemCommand {

    @Override
    public int execute(Path projectDirectory, BuildTool buildTool) {
        try {
            new ListPlatforms(
                    QuarkusProjectHelper.getProject(projectDirectory, buildTool == null ? BuildTool.MAVEN : buildTool,
                            QuarkusCliVersion.version()))
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

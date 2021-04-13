package io.quarkus.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.cli.core.BuildsystemCommand;
import io.quarkus.devtools.commands.RemoveExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "remove", aliases = "rm", mixinStandardHelpOptions = false, description = "Remove an extension from this project.")
public class Remove extends BaseSubCommand implements BuildsystemCommand {

    @CommandLine.Parameters(arity = "1", paramLabel = "EXTENSION", description = "extensions to remove")
    Set<String> extensions;

    @Override
    public boolean aggregate(BuildTool buildtool) {
        return buildtool != BuildTool.MAVEN;
    }

    @Override
    public List<String> getArguments(Path projectDir, BuildTool buildtool) {
        if (buildtool == BuildTool.MAVEN)
            throw new IllegalStateException("Should be unreachable");

        ArrayList<String> args = new ArrayList<>();
        args.add("removeExtension");
        String param = "--extensions=" + String.join(",", extensions);
        args.add(param);
        return args;
    }

    @Override
    public int execute(Path projectDir, BuildTool buildtool) throws Exception {
        if (buildtool == BuildTool.MAVEN) {
            return removeMaven(projectDir);
        } else {
            throw new IllegalStateException("Should be unreachable");
        }
    }

    private Integer removeMaven(Path projectDirectory) {
        try {
            RemoveExtensions project = new RemoveExtensions(
                    QuarkusCliUtils.getQuarkusProject(projectDirectory))
                            .extensions(extensions);
            QuarkusCommandOutcome result = project.execute();
            return result.isSuccess() ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
        } catch (Exception e) {
            if (parent.showErrors)
                e.printStackTrace(err());
            err().println("Unable to remove extension matching:" + e.getMessage());
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

}

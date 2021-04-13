package io.quarkus.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.cli.core.BuildsystemCommand;
import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import picocli.CommandLine;

@CommandLine.Command(name = "add", mixinStandardHelpOptions = false, description = "Add extension(s) to current project.")
public class Add extends BaseSubCommand implements BuildsystemCommand {

    @CommandLine.Parameters(arity = "1", paramLabel = "EXTENSION", description = "extensions to add to project")
    Set<String> extensions;

    @Override
    public boolean aggregate(BuildTool buildtool) {
        return buildtool != BuildTool.MAVEN;
    }

    @Override
    public int execute(Path projectDir, BuildTool buildtool) {
        if (buildtool == BuildTool.MAVEN) {
            return addMaven(projectDir);
        } else {
            throw new IllegalStateException("Should be unreachable");
        }
    }

    @Override
    public List<String> getArguments(Path projectDir, BuildTool buildtool) {
        if (buildtool == BuildTool.MAVEN)
            throw new IllegalStateException("Should be unreachable");
        ArrayList<String> args = new ArrayList<>();
        args.add("addExtension");
        String param = "--extensions=" + String.join(",", extensions);
        args.add(param);
        return args;
    }

    private Integer addMaven(Path projectDirectory) {
        try {
            final QuarkusProject quarkusProject = QuarkusCliUtils.getQuarkusProject(projectDirectory);

            AddExtensions project = new AddExtensions(quarkusProject);
            project.extensions(extensions);

            QuarkusCommandOutcome result = project.execute();
            return result.isSuccess() ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
        } catch (Exception e) {
            if (parent.showErrors)
                e.printStackTrace(err());
            err().println("Unable to add extension" + (extensions.size() > 1 ? "s" : "") + ": " + e.getMessage());
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

}

package io.quarkus.cli;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.cli.core.BuildsystemCommand;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "clean", mixinStandardHelpOptions = false, description = "Clean current project")
public class Clean extends BaseSubCommand implements BuildsystemCommand {

    @Override
    public boolean aggregate(BuildTool buildtool) {
        return true;
    }

    @Override
    public List<String> getArguments(Path projectDir, BuildTool buildtool) {
        if (buildtool == BuildTool.MAVEN) {
            return Collections.singletonList("clean");
        } else {
            return Collections.singletonList("clean");
        }
    }
}

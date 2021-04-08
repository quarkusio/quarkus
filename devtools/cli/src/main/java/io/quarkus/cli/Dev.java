package io.quarkus.cli;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.cli.core.BuildsystemCommand;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "dev", mixinStandardHelpOptions = false, description = "Execute project in live coding dev mode")
public class Dev extends BaseSubCommand implements BuildsystemCommand {

    @Override
    public boolean aggregate(BuildTool buildtool) {
        return true;
    }

    @Override
    public List<String> getArguments(Path projectDir, BuildTool buildtool) {
        if (buildtool == BuildTool.MAVEN) {
            return Collections.singletonList("quarkus:dev");
        } else {
            return Collections.singletonList("quarkusDev");
        }
    }
}

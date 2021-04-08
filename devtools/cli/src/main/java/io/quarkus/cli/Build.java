package io.quarkus.cli;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.cli.core.BuildsystemCommand;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "build", mixinStandardHelpOptions = false, description = "Build your quarkus project")
public class Build extends BaseSubCommand implements BuildsystemCommand {

    @CommandLine.Option(names = { "-n",
            "--native" }, order = 4, description = "Build native executable.")
    boolean isNative = false;

    @CommandLine.Option(names = { "-s",
            "--skip-tests" }, order = 5, description = "Skip tests.")
    boolean skipTests = false;

    @CommandLine.Option(names = { "--offline" }, order = 6, description = "Work offline.")
    boolean offline = false;

    @Override
    public boolean aggregate(BuildTool buildtool) {
        return true;
    }

    @Override
    public List<String> getArguments(Path projectDir, BuildTool buildtool) {
        LinkedList<String> args = new LinkedList<>();
        if (buildtool == BuildTool.MAVEN) {
            args.add("install");
            if (isNative)
                args.add("-Dnative");
            if (skipTests) {
                args.add("-DskipTests");
                args.add("-Dmaven.test.skip=true");
            }
            if (offline)
                args.add("--offline");
        } else {
            args.add("build");
            if (isNative)
                args.add("-Dquarkus.package.type=native");
            if (skipTests) {
                args.add("-x");
                args.add("test");
            }
            if (offline)
                args.add("--offline");
        }
        return args;
    }
}

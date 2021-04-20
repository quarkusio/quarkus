package io.quarkus.cli;

import java.nio.file.Path;
import java.util.ArrayList;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.cli.core.BuildsystemCommand;
import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "list", sortOptions = false, mixinStandardHelpOptions = false, description = "List installed (default) or installable extensions.")
public class List extends BaseSubCommand implements BuildsystemCommand {

    @CommandLine.Option(names = { "-i",
            "--installable" }, defaultValue = "false", order = 2, description = "Display installable extensions.")
    boolean installable = false;

    @CommandLine.Option(names = { "-s",
            "--search" }, defaultValue = "*", paramLabel = "PATTERN", order = 3, description = "Search filter on extension list. The format is based on Java Pattern.")
    String searchPattern;

    @CommandLine.ArgGroup()
    ExtensionFormat format = new ExtensionFormat();

    static class ExtensionFormat {
        @CommandLine.Option(names = { "--name" }, order = 4, description = "Display extension name only. (default)")
        boolean name = false;

        @CommandLine.Option(names = { "--concise" }, order = 5, description = "Display extension name and description.")
        boolean concise = false;

        @CommandLine.Option(names = {
                "--full" }, order = 6, description = "Display concise format and version related columns.")
        boolean full = false;

        @CommandLine.Option(names = {
                "--origins" }, order = 7, description = "Display extensions including their platform origins.")
        boolean origins = false;

    }

    @Override
    public boolean aggregate(BuildTool buildtool) {
        return buildtool != BuildTool.MAVEN;
    }

    @Override
    public int execute(Path projectDirectory, BuildTool buildtool) {
        if (buildtool == BuildTool.MAVEN) {
            return listExtensionsMaven(projectDirectory);
        } else {
            throw new IllegalStateException("Should be unreachable");
        }
    }

    private String getFormatString() {
        String formatString = "name";
        if (format.concise)
            formatString = "concise";
        else if (format.full)
            formatString = "full";
        else if (format.origins)
            formatString = "origins";
        return formatString;
    }

    @Override
    public java.util.List<String> getArguments(Path projectDir, BuildTool buildtool) {
        if (buildtool == BuildTool.MAVEN)
            throw new IllegalStateException("Should be unreachable");
        ArrayList<String> args = new ArrayList<>();
        args.add("listExtensions");
        args.add("--fromCli");
        args.add("--format=" + getFormatString());
        if (!installable)
            args.add("--installed");
        if (searchPattern != null)
            args.add("--searchPattern=" + searchPattern);

        return args;
    }

    private Integer listExtensionsMaven(Path projectDirectory) {
        // we do not have to spawn process for maven
        try {

            new ListExtensions(QuarkusCliUtils.getQuarkusProject(projectDirectory))
                    .fromCli(true)
                    .all(false)
                    .installed(!installable)
                    .format(getFormatString())
                    .search(searchPattern)
                    .execute();
        } catch (QuarkusCommandException e) {
            if (parent.showErrors)
                e.printStackTrace(err());
            return CommandLine.ExitCode.SOFTWARE;
        } catch (IllegalStateException e) {
            if (parent.showErrors)
                e.printStackTrace(err());
            err().println("No project exists to list extensions from.");
            return CommandLine.ExitCode.SOFTWARE;
        }
        return CommandLine.ExitCode.OK;
    }

}

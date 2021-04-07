package io.quarkus.cli;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import io.quarkus.cli.core.BuildsystemCommand;
import io.quarkus.cli.core.ExecuteUtil;
import io.quarkus.cli.core.QuarkusCliVersion;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

@QuarkusMain
@CommandLine.Command(name = "qs", versionProvider = QuarkusCliVersion.class, subcommandsRepeatable = true, mixinStandardHelpOptions = true, subcommands = {
        Build.class,
        Clean.class, Create.class, CreateJBang.class, List.class, Platforms.class, Add.class, Remove.class, Dev.class,
        CreateExtension.class })
public class QuarkusCli implements QuarkusApplication {

    public void usage() {
        CommandLine.usage(this, System.out);

    }

    @CommandLine.Option(names = { "-e", "--errors" }, description = "Produce execution error messages.")
    boolean showErrors;

    @CommandLine.Option(names = { "--verbose" }, description = "Verbose mode.")
    boolean verbose;

    @CommandLine.Option(names = { "--manual-output" }, hidden = true, description = "For unit test purposes.")
    boolean manualOutput;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    public PrintWriter out() {
        return spec.commandLine().getOut();
    }

    public PrintWriter err() {
        return spec.commandLine().getErr();
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isShowErrors() {
        return showErrors;
    }

    public boolean isManualOutput() {
        return manualOutput;
    }

    @Inject
    CommandLine.IFactory factory;

    private Path projectDirectory;

    public Path getProjectDirectory() {
        if (projectDirectory == null) {
            projectDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        }
        return projectDirectory;
    }

    public void setProjectDirectory(Path projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    @Override
    public int run(String... args) throws Exception {
        CommandLine cmd = factory == null ? new CommandLine(this) : new CommandLine(this, factory);
        return cmd.setExecutionStrategy(new ExecutionStrategy())
                .execute(args);
    }

    public BuildTool resolveBuildTool(Path projectDirectory) {
        return QuarkusProject.resolveExistingProjectBuildTool(projectDirectory.toAbsolutePath());
    }

    public int executeBuildsystem(BuildTool buildtool, java.util.List<String> args) throws Exception {
        String[] newArgs = args.toArray(new String[args.size()]);
        if (buildtool == BuildTool.MAVEN) {
            return ExecuteUtil.executeMavenTarget(getProjectDirectory().toFile(), this, newArgs);
        } else {
            return ExecuteUtil.executeGradleTarget(getProjectDirectory().toFile(), this, newArgs);
        }
    }

    class ExecutionStrategy implements CommandLine.IExecutionStrategy {
        @Override
        public int execute(CommandLine.ParseResult parseResult)
                throws CommandLine.ExecutionException, CommandLine.ParameterException {
            try {
                // Aggregate any maven/gradle commands into one build process
                // if BuildsystemCommand.aggregate() returns true
                LinkedList<String> buildsystemArguments = new LinkedList<>();
                BuildTool buildtool = null;
                if (parseResult.hasSubcommand()) {
                    for (CommandLine.ParseResult pr : parseResult.subcommands()) {
                        if (CommandLine.printHelpIfRequested(pr)) {
                            return CommandLine.ExitCode.USAGE;
                        }
                        // TODO: More recursion will be needed if any subcommand has sub-sub commands
                        for (CommandLine cl : pr.asCommandLineList()) {
                            try {
                                Object cmd = cl.getCommand();
                                if (cmd instanceof BuildsystemCommand) {
                                    if (buildtool == null)
                                        buildtool = resolveBuildTool(getProjectDirectory());
                                    BuildsystemCommand build = (BuildsystemCommand) cmd;
                                    if (build.aggregate(buildtool)) {
                                        buildsystemArguments.addAll(build.getArguments(getProjectDirectory(), buildtool));
                                    } else {
                                        if (buildsystemArguments.size() > 0) {
                                            int exitCode = executeBuildsystem(buildtool, buildsystemArguments);
                                            if (exitCode != CommandLine.ExitCode.OK)
                                                return exitCode;
                                            buildsystemArguments.clear();
                                        }

                                        int exitCode = build.execute(getProjectDirectory(), buildtool);
                                        if (exitCode != CommandLine.ExitCode.OK)
                                            return exitCode;
                                    }
                                } else if (cmd instanceof Callable) {
                                    int exitCode = ((Callable<Integer>) cmd).call();
                                    if (exitCode != CommandLine.ExitCode.OK)
                                        return exitCode;
                                } else {
                                    err().println("Unknown execution type for : " + cl.getCommandName());
                                    return CommandLine.ExitCode.SOFTWARE;
                                }
                            } catch (Exception e) {
                                if (showErrors)
                                    e.printStackTrace(err());
                                err().println("Failure executing command : " + cl.getCommandName());
                                return CommandLine.ExitCode.SOFTWARE;
                            }
                        }
                    }
                    if (buildsystemArguments.size() > 0) {
                        try {
                            return executeBuildsystem(buildtool, buildsystemArguments);
                        } catch (Exception e) {
                            if (showErrors)
                                e.printStackTrace(err());
                            err().print("Failure executing build command : ");
                            ExecuteUtil.outputBuildCommand(err(), buildtool, buildsystemArguments);
                            err().println();
                            return CommandLine.ExitCode.SOFTWARE;
                        }
                    }
                } else {
                    if (parseResult.isVersionHelpRequested()) {
                        parseResult.asCommandLineList().iterator().next().printVersionHelp(out());
                    } else {
                        // no subcommands executed
                        usage();
                    }
                }
                return CommandLine.ExitCode.OK;
            } catch (Exception e) {
                if (showErrors)
                    e.printStackTrace(err());
                return CommandLine.ExitCode.SOFTWARE;
            }
        }

    }

}

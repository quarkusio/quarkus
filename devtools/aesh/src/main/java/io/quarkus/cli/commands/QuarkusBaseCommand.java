package io.quarkus.cli.commands;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.terminal.utils.Config;

@GroupCommandDefinition(name = QuarkusBaseCommand.COMMAND_NAME, generateHelp = true, groupCommands = {
        ListExtensionsCommand.class,
        AddExtensionsCommand.class,
        CreateProjectCommand.class,
        DevModeCommand.class,
        CompileProjectCommand.class }, description = "<command> [<args>] \n\nThese are the common quarkus commands used in various situations")
public class QuarkusBaseCommand implements Command<CommandInvocation> {
    public static final String COMMAND_NAME = "quarkus";

    @Option(shortName = 'i', hasValue = false, description = "Starts an interactive Quarkus Shell")
    private boolean interactive;

    public CommandResult execute(CommandInvocation commandInvocation) {
        if (interactive) {
            commandInvocation.println("Starting interactive CLI....");
            //we need to stop first since the QuarkusCli starts the runner with interactive = true
            commandInvocation.stop();
            startInteractive();
        }

        return CommandResult.SUCCESS;
    }

    private void startInteractive() {
        //we start the CLI in a new thread since the caller has a TerminalConnection open
        Runnable runnable = () -> {
            AeshConsoleRunner.builder()
                    .command(CreateProjectCommand.class)
                    .command(AddExtensionsCommand.class)
                    .command(ListExtensionsCommand.class)
                    .command(DevModeCommand.class)
                    .command(CompileProjectCommand.class)
                    .prompt("[quarkus@" + Config.getUserDir() + "]$ ")
                    .addExitCommand()
                    .start();
        };
        new Thread(runnable).start();
    }
}

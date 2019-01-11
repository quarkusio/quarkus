package org.jboss.shamrock.cli;

import java.io.IOException;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.jboss.shamrock.cli.commands.ProteanCommand;

public class ProteanCli {

    public static void main(String[] args) throws CommandLineParserException {
        CommandRuntime runtime = AeshCommandRuntimeBuilder
                                         .builder()
                                         .commandRegistry(AeshCommandRegistryBuilder.builder().command(ProteanCommand.class).create())
                                         .build();

        if (args.length > 0) {
            StringBuilder sb = new StringBuilder(ProteanCommand.COMMAND_NAME).append(" ");
            if (args.length == 1) {
                sb.append(args[0]);
            } else {
                for (String arg : args) {
                    if (arg.indexOf(' ') >= 0) {
                        sb.append('"').append(arg).append("\" ");
                    } else {
                        sb.append(arg).append(' ');
                    }
                }
            }

            try {
                runtime.executeCommand(sb.toString());
            } catch (CommandNotFoundException e) {
                System.err.println("Command not found: " + sb.toString());
            } catch (CommandException | CommandLineParserException | CommandValidatorException | OptionValidatorException e) {
                showHelpIfNeeded(runtime, e);
            } catch (InterruptedException | IOException e) {
                System.err.println(e.getMessage());
            }
        } else {
            showHelpIfNeeded(runtime, null);
        }
    }

    private static void showHelpIfNeeded(CommandRuntime runtime, Exception e) {
        if (e != null) {
            System.err.println(e.getMessage());
        }
        System.err.println(runtime.commandInfo(ProteanCommand.COMMAND_NAME));
    }
}

package org.jboss.shamrock.cli;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.jboss.shamrock.cli.commands.ProteanCommand;

import java.io.IOException;

public class ProteanCli {

    public static void main(String[] args) throws CommandLineParserException {
        CommandRuntime runtime = AeshCommandRuntimeBuilder
                                         .builder()
                                         .commandRegistry(AeshCommandRegistryBuilder.builder().command(ProteanCommand.class).create())
                                         .build();

        StringBuilder sb = new StringBuilder();
        if (args.length == 1) {
            sb.append(args[0]);
        }
        else {
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
        }
        catch (CommandNotFoundException e) {
            System.out.println("Command not found: "+sb.toString());

        }
        catch (OptionValidatorException | CommandException | CommandValidatorException | IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

}

package io.quarkus.claesh;

import java.io.IOException;

import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.registry.CommandRegistryException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;

import io.quarkus.arc.Arc;

public class AeshCommandRunner {

    public void run(Class<QuarkusCommand>[] commands, String[] args) {
        try {
            final QuarkusContext quarkusContext = new QuarkusContext(Arc.container());

            final CommandRegistry<QuarkusCommandInvocation> registry = AeshCommandRegistryBuilder
                    .<QuarkusCommandInvocation> builder()
                    .commands(commands)
                    .create();

            final CommandRuntime<QuarkusCommandInvocation> runtime = AeshCommandRuntimeBuilder
                    .<QuarkusCommandInvocation> builder()
                    .commandRegistry(registry)
                    .commandInvocationProvider(new QuarkusCommandInvocationProvider(quarkusContext))
                    .build();

            final StringBuilder sb = new StringBuilder();
            if (args.length > 0) {
                sb.append(" ");
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
            }
            runtime.executeCommand(sb.toString());
        }
        //simplified exceptions for now
        catch (CommandNotFoundException | CommandException | CommandLineParserException | CommandValidatorException
                | OptionValidatorException | InterruptedException | IOException | CommandRegistryException e) {
            throw new RuntimeException(e);
        }
    }
}

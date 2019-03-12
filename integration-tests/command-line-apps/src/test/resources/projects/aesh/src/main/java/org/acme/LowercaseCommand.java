package org.acme;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.claesh.QuarkusCommand;
import io.quarkus.claesh.QuarkusCommandInvocation;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;

@CommandDefinition(name = "lowercase", description = "lowercaser")
public class LowercaseCommand implements QuarkusCommand {

    @Option
    private String file;

    @Arguments
    private List<String> arguments;

    @Override
    public CommandResult execute(QuarkusCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        final Path out = Paths.get(file);

        try {
            final Lowercaser capitalizer = commandInvocation.quarkusContext().getArcContainer().instance(Lowercaser.class).get();
            Files.write(out, arguments.stream().map(capitalizer::perform).collect(Collectors.toList()), Charset.defaultCharset());
        } catch (IOException e) {
            throw new CommandException(e);
        }

        return CommandResult.SUCCESS;
    }
}

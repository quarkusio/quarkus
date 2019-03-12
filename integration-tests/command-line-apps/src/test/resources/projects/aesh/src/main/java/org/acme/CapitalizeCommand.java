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

@CommandDefinition(name = "capitalize", description = "capitalizer")
public class CapitalizeCommand implements QuarkusCommand {

    @Option
    private String file;

    @Arguments
    private List<String> arguments;

    @Override
    public CommandResult execute(QuarkusCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        final Path out = Paths.get(file);

        try {
            final Capitalizer capitalizer = commandInvocation.quarkusContext().getArcContainer().instance(Capitalizer.class).get();
            Files.write(out, arguments.stream().map(capitalizer::perform).collect(Collectors.toList()), Charset.defaultCharset());
        } catch (IOException e) {
            throw new CommandException(e);
        }

        return CommandResult.SUCCESS;
    }
}

package io.quarkus.it.aesh;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;

/**
 * A pipe-friendly command that reads stdin and prints each line in uppercase.
 * Used for pipeline integration tests. Returns FAILURE (instead of silently
 * succeeding) when stdin cannot be read, so terminal-stage failures are
 * observable in stage results.
 */
@CommandDefinition(name = "upper", description = "Uppercase lines from stdin")
public class UpperCommand implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        try {
            java.io.InputStream stdin = invocation.getStdin();
            if (stdin != null) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stdin, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    invocation.println(line.toUpperCase());
                }
            }
        } catch (Exception e) {
            return CommandResult.FAILURE;
        }
        return CommandResult.SUCCESS;
    }
}

package io.quarkus.it.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;

/**
 * A pipe-friendly upstream command that writes a partial line then throws.
 * Used to test that upstream pipeline failures are visible in stage results.
 * <p>
 * Named {@code failpipe} on the CLI to distinguish it from the existing
 * {@code fail} command ({@code FailCommand}); the "partial" line flows
 * downstream, so {@code failpipe | upper} outputs {@code PARTIAL}.
 */
@CommandDefinition(name = "failpipe", description = "Upstream that writes then fails")
public class FailPipeCommand implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation) throws CommandException {
        invocation.println("partial");
        throw new CommandException("upstream boom");
    }
}

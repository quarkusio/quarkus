package io.quarkus.devtools.commands.handlers;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;

public interface QuarkusCommandHandler {

    QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException;
}

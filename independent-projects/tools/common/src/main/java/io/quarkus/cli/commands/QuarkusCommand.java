package io.quarkus.cli.commands;

public interface QuarkusCommand {

    QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException;
}

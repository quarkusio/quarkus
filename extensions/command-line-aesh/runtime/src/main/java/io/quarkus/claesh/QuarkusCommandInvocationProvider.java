package io.quarkus.claesh;

import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationProvider;

public class QuarkusCommandInvocationProvider implements CommandInvocationProvider<QuarkusCommandInvocation> {

    private final QuarkusContext context;

    public QuarkusCommandInvocationProvider(QuarkusContext context) {
        this.context = context;
    }

    @Override
    public QuarkusCommandInvocation enhanceCommandInvocation(CommandInvocation commandInvocation) {
        return new QuarkusCommandInvocation(commandInvocation, context);
    }
}

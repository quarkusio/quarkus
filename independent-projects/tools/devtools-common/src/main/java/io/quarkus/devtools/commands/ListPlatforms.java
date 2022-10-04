package io.quarkus.devtools.commands;

import java.util.HashMap;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.ListPlatformsCommandHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;

public class ListPlatforms {

    private final QuarkusCommandInvocation invocation;
    private final ListPlatformsCommandHandler handler = new ListPlatformsCommandHandler();

    public ListPlatforms(QuarkusProject quarkusProject) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject);
    }

    public ListPlatforms(QuarkusProject quarkusProject, MessageWriter messageWriter) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject, new HashMap<>(), messageWriter);
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return handler.execute(invocation);
    }
}

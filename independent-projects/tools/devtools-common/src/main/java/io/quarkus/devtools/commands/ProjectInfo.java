package io.quarkus.devtools.commands;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.ProjectInfoCommandHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;

/**
 * Instances of this class are not thread-safe. They are created per single invocation.
 */
public class ProjectInfo {

    public static final String APP_MODEL = "quarkus.project-info.app-model";
    public static final String PER_MODULE = "quarkus.project-info.per-module";

    private final QuarkusCommandInvocation invocation;
    private final ProjectInfoCommandHandler handler = new ProjectInfoCommandHandler();

    public ProjectInfo(final QuarkusProject quarkusProject) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject);
    }

    public ProjectInfo(final QuarkusProject quarkusProject, final MessageWriter messageWriter) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject, new HashMap<>(), messageWriter);
    }

    public ProjectInfo appModel(ApplicationModel applicationModel) {
        invocation.setValue(APP_MODEL, requireNonNull(applicationModel, "applicationModel is required"));
        return this;
    }

    public ProjectInfo perModule(boolean perModule) {
        invocation.setValue(PER_MODULE, perModule);
        return this;
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return handler.execute(invocation);
    }
}

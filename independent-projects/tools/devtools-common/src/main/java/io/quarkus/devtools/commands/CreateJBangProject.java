package io.quarkus.devtools.commands;

import static io.quarkus.devtools.project.codegen.ProjectGenerator.EXTENSIONS;
import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.CreateJBangProjectCommandHandler;
import io.quarkus.devtools.project.QuarkusProject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreateJBangProject {
    public static final String NAME = "create-jbang";

    private final QuarkusProject quarkusProject;

    private Set<String> extensions = new HashSet<>();
    private Map<String, Object> values = new HashMap<>();

    public CreateJBangProject(QuarkusProject quarkusProject) {
        this.quarkusProject = requireNonNull(quarkusProject, "quarkusProject is required");
    }

    public CreateJBangProject extensions(Set<String> extensions) {
        if (extensions == null) {
            return this;
        }
        this.extensions.addAll(extensions);
        return this;
    }

    public CreateJBangProject setValue(String name, Object value) {
        if (value != null) {
            values.put(name, value);
        }
        return this;
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        setValue(EXTENSIONS, extensions);
        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject, values);
        return new CreateJBangProjectCommandHandler().execute(invocation);
    }
}

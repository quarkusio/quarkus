package io.quarkus.devtools.commands;

import static io.quarkus.devtools.project.JavaVersion.computeJavaVersion;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.devtools.commands.CreateProject.CreateProjectKey;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.CreateJBangProjectCommandHandler;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.SourceType;

public class CreateJBangProject {
    public interface CreateJBangProjectKey {
        String NO_JBANG_WRAPPER = "codegen.no-jbang-wrapper";
    }

    public static final String NAME = "create-jbang";

    private final QuarkusProject quarkusProject;

    private Set<String> extensions = new HashSet<>();
    private Map<String, Object> values = new HashMap<>();
    private String javaVersion;

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

    public CreateJBangProject javaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
        return this;
    }

    public CreateJBangProject setValue(String name, Object value) {
        if (value != null) {
            values.put(name, value);
        }
        return this;
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        setValue(CreateProjectKey.EXTENSIONS, extensions);
        final SourceType sourceType = SourceType.resolve(extensions);
        setValue(CreateProjectKey.JAVA_VERSION, computeJavaVersion(sourceType, javaVersion)); // default

        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject, values);
        return new CreateJBangProjectCommandHandler().execute(invocation);
    }
}

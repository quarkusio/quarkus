package io.quarkus.devtools.commands;

import static io.quarkus.devtools.project.codegen.ProjectGenerator.EXTENSIONS;
import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.CreateJBangProjectCommandHandler;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreateJBangProject {
    public static final String NAME = "create-jbang";

    private final Path projectDirPath;
    private final QuarkusPlatformDescriptor platformDescr;
    private BuildTool buildTool = BuildTool.MAVEN;

    private Set<String> extensions = new HashSet<>();
    private Map<String, Object> values = new HashMap<>();

    public CreateJBangProject(Path projectDirPath, QuarkusPlatformDescriptor platformDescr) {
        this.projectDirPath = requireNonNull(projectDirPath, "projectDirPath is required");
        this.platformDescr = requireNonNull(platformDescr, "platformDescr is required");
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
        final QuarkusProject quarkusProject = QuarkusProject.of(projectDirPath, platformDescr, buildTool);
        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject, values);
        return new CreateJBangProjectCommandHandler().execute(invocation);
    }
}

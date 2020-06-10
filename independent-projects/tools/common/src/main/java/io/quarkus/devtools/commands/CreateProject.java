package io.quarkus.devtools.commands;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.CLASS_NAME;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.EXTENSIONS;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.IS_SPRING;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.JAVA_TARGET;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PROJECT_ARTIFACT_ID;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PROJECT_GROUP_ID;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PROJECT_VERSION;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.SOURCE_TYPE;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.CreateProjectCommandHandler;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;

/**
 * Instances of this class are not thread-safe. They are created per invocation.
 *
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class CreateProject {

    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("(?:1\\.)?(\\d+)(?:\\..*)?");

    private final Path projectFolderPath;
    private final QuarkusPlatformDescriptor platformDescr;
    private String javaTarget;
    private BuildTool buildTool = BuildTool.MAVEN;

    private Map<String, Object> values = new HashMap<>();

    public CreateProject(final Path projectFolderPath, QuarkusPlatformDescriptor platformDescr) {
        this.projectFolderPath = checkNotNull(projectFolderPath, "projectFolderPath is required");
        this.platformDescr = checkNotNull(platformDescr, "platformDescr is required");
    }

    public CreateProject groupId(String groupId) {
        setValue(PROJECT_GROUP_ID, groupId);
        return this;
    }

    public CreateProject artifactId(String artifactId) {
        setValue(PROJECT_ARTIFACT_ID, artifactId);
        return this;
    }

    public CreateProject version(String version) {
        setValue(PROJECT_VERSION, version);
        return this;
    }

    public CreateProject sourceType(SourceType sourceType) {
        setValue(SOURCE_TYPE, sourceType);
        return this;
    }

    public CreateProject javaTarget(String javaTarget) {
        this.javaTarget = javaTarget;
        return this;
    }

    public CreateProject className(String className) {
        if (className == null) {
            return this;
        }
        if (!(SourceVersion.isName(className) && !SourceVersion.isKeyword(className))) {
            throw new IllegalArgumentException(className + " is not a valid class name");
        }
        setValue(CLASS_NAME, className);
        return this;
    }

    public CreateProject extensions(Set<String> extensions) {
        if (isSpringStyle(extensions)) {
            setValue(IS_SPRING, true);
        }
        setValue(EXTENSIONS, extensions);
        return this;
    }

    public CreateProject setValue(String name, Object value) {
        values.put(name, value);
        return this;
    }

    public CreateProject buildTool(BuildTool buildTool) {
        this.buildTool = checkNotNull(buildTool, "buildTool is required");
        return this;
    }

    public boolean doCreateProject(final Map<String, Object> context) throws IOException {
        if (context != null && !context.isEmpty()) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (entry.getValue() != null) {
                    setValue(entry.getKey(), entry.getValue());
                }
            }
        }

        try {
            return execute().isSuccess();
        } catch (QuarkusCommandException e) {
            throw new IOException("Failed to create project", e);
        }
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        // Define the Java version to use determined from the one specified or the one creating the project
        Matcher matcher = JAVA_VERSION_PATTERN
                .matcher(this.javaTarget != null ? this.javaTarget : System.getProperty("java.version", ""));
        if (matcher.matches() && Integer.parseInt(matcher.group(1)) < 11) {
            setValue(JAVA_TARGET, "8");
        } else {
            setValue(JAVA_TARGET, "11");
        }

        final QuarkusProject quarkusProject = QuarkusProject.of(projectFolderPath, platformDescr, buildTool);
        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject, values);
        return new CreateProjectCommandHandler().execute(invocation);
    }

    public static SourceType determineSourceType(Set<String> extensions) {
        Optional<SourceType> sourceType = extensions.stream()
                .map(SourceType::parse)
                .filter(Optional::isPresent)
                .map(e -> e.orElse(SourceType.JAVA))
                .findAny();
        return sourceType.orElse(SourceType.JAVA);
    }

    private static boolean isSpringStyle(Collection<String> extensions) {
        return extensions != null && extensions.stream().anyMatch(e -> e.toLowerCase().contains("spring-web"));
    }
}

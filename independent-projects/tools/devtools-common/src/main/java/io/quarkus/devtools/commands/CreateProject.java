package io.quarkus.devtools.commands;

import static io.quarkus.devtools.project.codegen.ProjectGenerator.*;
import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.CreateProjectCommandHandler;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
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

    public static final String NAME = "create-project";

    public static final String CODESTARTS_ENABLED = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "codestarts-enabled");
    public static final String NO_DOCKERFILES = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "no-dockerfiles");
    public static final String NO_BUILDTOOL_WRAPPER = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "no-buildtool-wrapper");
    public static final String NO_EXAMPLES = ToolsUtils.dotJoin(ToolsConstants.QUARKUS, NAME, "no-examples");

    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("(?:1\\.)?(\\d+)(?:\\..*)?");

    private final Path projectDirPath;
    private final QuarkusPlatformDescriptor platformDescr;
    private String javaTarget;
    private BuildTool buildTool = BuildTool.MAVEN;

    private Map<String, Object> values = new HashMap<>();

    public CreateProject(final Path projectDirPath, QuarkusPlatformDescriptor platformDescr) {
        this.projectDirPath = requireNonNull(projectDirPath, "projectDirPath is required");
        this.platformDescr = requireNonNull(platformDescr, "platformDescr is required");
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

    public CreateProject quarkusMavenPluginVersion(String version) {
        setValue(QUARKUS_MAVEN_PLUGIN_VERSION, version);
        return this;
    }

    public CreateProject quarkusGradlePluginVersion(String version) {
        setValue(QUARKUS_GRADLE_PLUGIN_VERSION, version);
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

    public CreateProject codestartsEnabled(boolean value) {
        setValue(CODESTARTS_ENABLED, value);
        return this;
    }

    public CreateProject codestartsEnabled() {
        return codestartsEnabled(true);
    }

    public CreateProject noExamples(boolean value) {
        setValue(NO_EXAMPLES, value);
        return this;
    }

    public CreateProject noExamples() {
        return noExamples(true);
    }

    public CreateProject noBuildToolWrapper(boolean value) {
        setValue(NO_BUILDTOOL_WRAPPER, value);
        return this;
    }

    public CreateProject noBuildToolWrapper() {
        return noBuildToolWrapper(true);
    }

    public CreateProject noDockerfiles(boolean value) {
        setValue(NO_DOCKERFILES, value);
        return this;
    }

    public CreateProject noDockerfiles() {
        return noDockerfiles(true);
    }

    public CreateProject setValue(String name, Object value) {
        if (value != null) {
            values.put(name, value);
        }
        return this;
    }

    public CreateProject buildTool(BuildTool buildTool) {
        this.buildTool = requireNonNull(buildTool, "buildTool is required");
        return this;
    }

    public boolean doCreateProject(final Map<String, Object> context) throws QuarkusCommandException {
        if (context != null && !context.isEmpty()) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (entry.getValue() != null) {
                    setValue(entry.getKey(), entry.getValue());
                }
            }
        }
        return execute().isSuccess();
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

        final QuarkusProject quarkusProject = QuarkusProject.of(projectDirPath, platformDescr, buildTool);
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

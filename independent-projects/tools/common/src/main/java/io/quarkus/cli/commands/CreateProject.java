package io.quarkus.cli.commands;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.quarkus.generators.ProjectGenerator.CLASS_NAME;
import static io.quarkus.generators.ProjectGenerator.IS_SPRING;
import static io.quarkus.generators.ProjectGenerator.JAVA_TARGET;
import static io.quarkus.generators.ProjectGenerator.PROJECT_ARTIFACT_ID;
import static io.quarkus.generators.ProjectGenerator.PROJECT_GROUP_ID;
import static io.quarkus.generators.ProjectGenerator.PROJECT_VERSION;
import static io.quarkus.generators.ProjectGenerator.SOURCE_TYPE;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.generators.SourceType;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
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

    private final ProjectWriter writer;
    private final QuarkusPlatformDescriptor platformDescr;
    private String javaTarget;
    private BuildFile buildFile;
    private BuildTool buildTool = BuildTool.MAVEN;

    private Map<String, Object> values = new HashMap<>();

    public CreateProject(final ProjectWriter writer, QuarkusPlatformDescriptor platformDescr) {
        this.writer = checkNotNull(writer, "writer is required");
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

    public CreateProject buildFile(BuildFile buildFile) {
        this.buildFile = buildFile;
        this.buildTool(buildFile.getBuildTool());
        return this;
    }

    /**
     * @deprecated in 1.3.0.CR
     */
    @Deprecated
    public CreateProject extensions(Set<String> extensions) {
        if (isSpringStyle(extensions)) {
            setValue(IS_SPRING, true);
        }
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

        final BuildFile computeBuildFile = computeBuildFile();
        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(values, platformDescr, writer,
                computeBuildFile);
        return new CreateProjectCommandHandler().execute(invocation);
    }

    private BuildFile computeBuildFile() throws QuarkusCommandException {
        if (buildFile != null) {
            return buildFile;
        }
        if (buildTool != null) {
            try {
                return buildTool.createBuildFile(writer);
            } catch (final IOException e) {
                throw new QuarkusCommandException("Failed to create project", e);
            }
        }
        throw new QuarkusCommandException("Either BuildTool or BuildFile must be defined");
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

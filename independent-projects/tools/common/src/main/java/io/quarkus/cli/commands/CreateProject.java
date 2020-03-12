package io.quarkus.cli.commands;

import static io.quarkus.generators.ProjectGenerator.CLASS_NAME;
import static io.quarkus.generators.ProjectGenerator.IS_SPRING;
import static io.quarkus.generators.ProjectGenerator.PROJECT_ARTIFACT_ID;
import static io.quarkus.generators.ProjectGenerator.PROJECT_GROUP_ID;
import static io.quarkus.generators.ProjectGenerator.PROJECT_VERSION;
import static io.quarkus.generators.ProjectGenerator.SOURCE_TYPE;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.generators.SourceType;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.SourceVersion;

/**
 * Instances of this class are not thread-safe. They are created per invocation.
 *
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class CreateProject {

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

    private QuarkusCommandInvocation invocation;

    /**
     * @deprecated since 1.3.0.CR1
     *             Please use {@link #CreateProject(ProjectWriter, QuarkusPlatformDescriptor)} instead.
     */
    @Deprecated
    public CreateProject(ProjectWriter writer) {
        this(writer, QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor());
    }

    public CreateProject(final ProjectWriter writer, QuarkusPlatformDescriptor platformDescr) {
        invocation = new QuarkusCommandInvocation(platformDescr);
        invocation.setProjectWriter(writer);
    }

    public CreateProject groupId(String groupId) {
        setProperty(PROJECT_GROUP_ID, groupId);
        return this;
    }

    public CreateProject artifactId(String artifactId) {
        setProperty(PROJECT_ARTIFACT_ID, artifactId);
        return this;
    }

    public CreateProject version(String version) {
        setProperty(PROJECT_VERSION, version);
        return this;
    }

    public CreateProject sourceType(SourceType sourceType) {
        invocation.setValue(SOURCE_TYPE, sourceType);
        return this;
    }

    public CreateProject className(String className) {
        if (className == null) {
            return this;
        }
        if (!(SourceVersion.isName(className) && !SourceVersion.isKeyword(className))) {
            throw new IllegalArgumentException(className + " is not a valid class name");
        }
        setProperty(CLASS_NAME, className);
        return this;
    }

    /**
     * @deprecated in 1.3.0.CR
     */
    @Deprecated
    public CreateProject extensions(Set<String> extensions) {
        if (isSpringStyle(extensions)) {
            invocation.setValue(IS_SPRING, true);
        }
        return this;
    }

    public CreateProject setProperty(String name, String value) {
        invocation.setProperty(name, value);
        return this;
    }

    public CreateProject setValue(String name, Object value) {
        invocation.setValue(name, value);
        return this;
    }

    public CreateProject buildFile(BuildFile buildFile) {
        invocation.setBuildFile(buildFile);
        return this;
    }

    public CreateProject buildTool(BuildTool buildTool) {
        invocation.setBuildTool(buildTool);
        return this;
    }

    public boolean doCreateProject(final Map<String, Object> context) throws IOException {
        if (context != null && !context.isEmpty()) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (entry.getValue() != null) {
                    invocation.setProperty(entry.getKey(), entry.getValue().toString());
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
        return new CreateProjectCommandHandler().execute(invocation);
    }
}

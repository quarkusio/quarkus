package io.quarkus.cli.commands;

import static io.quarkus.generators.ProjectGenerator.BOM_ARTIFACT_ID;
import static io.quarkus.generators.ProjectGenerator.BOM_GROUP_ID;
import static io.quarkus.generators.ProjectGenerator.BOM_VERSION;
import static io.quarkus.generators.ProjectGenerator.BUILD_FILE;
import static io.quarkus.generators.ProjectGenerator.CLASS_NAME;
import static io.quarkus.generators.ProjectGenerator.IS_SPRING;
import static io.quarkus.generators.ProjectGenerator.PACKAGE_NAME;
import static io.quarkus.generators.ProjectGenerator.PROJECT_ARTIFACT_ID;
import static io.quarkus.generators.ProjectGenerator.PROJECT_GROUP_ID;
import static io.quarkus.generators.ProjectGenerator.PROJECT_VERSION;
import static io.quarkus.generators.ProjectGenerator.QUARKUS_VERSION;
import static io.quarkus.generators.ProjectGenerator.SOURCE_TYPE;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Model;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.legacy.LegacyQuarkusCommandInvocation;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.generators.ProjectGeneratorRegistry;
import io.quarkus.generators.SourceType;
import io.quarkus.generators.rest.BasicRestProjectGenerator;
import io.quarkus.platform.tools.ToolsUtils;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class CreateProject implements QuarkusCommand {

    private ProjectWriter writer;
    private String groupId;
    private String artifactId;
    private String version;
    private SourceType sourceType = SourceType.JAVA;
    private BuildFile buildFile;
    private BuildTool buildTool;
    private String className;
    private Set<String> extensions;

    private Model model;

    public CreateProject(final ProjectWriter writer) {
        this.writer = writer;
    }

    public CreateProject groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public CreateProject artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public CreateProject version(String version) {
        this.version = version;
        return this;
    }

    public CreateProject sourceType(SourceType sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    public CreateProject className(String className) {
        this.className = className;
        return this;
    }

    public CreateProject extensions(Set<String> extensions) {
        this.extensions = extensions;
        return this;
    }

    public CreateProject buildFile(BuildFile buildFile) {
        this.buildFile = buildFile;
        return this;
    }

    public CreateProject buildTool(BuildTool buildTool) {
        this.buildTool = buildTool;
        return this;
    }

    public Model getModel() {
        return model;
    }

    public boolean doCreateProject(final Map<String, Object> context) throws IOException {
        try {
            return execute(new LegacyQuarkusCommandInvocation(context)).isSuccess();
        } catch (QuarkusCommandException e) {
            throw new IOException("Failed to create project", e);
        }
    }

    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        if (!writer.init()) {
            return QuarkusCommandOutcome.failure();
        }

        final Properties quarkusProps = ToolsUtils.readQuarkusProperties(invocation.getPlatformDescriptor());
        quarkusProps.forEach((k, v) -> invocation.setProperty(k.toString().replace("-", "_"), v.toString()));

        invocation.setProperty(PROJECT_GROUP_ID, groupId);
        invocation.setProperty(PROJECT_ARTIFACT_ID, artifactId);
        invocation.setProperty(PROJECT_VERSION, version);
        invocation.setProperty(BOM_GROUP_ID, invocation.getPlatformDescriptor().getBomGroupId());
        invocation.setProperty(BOM_ARTIFACT_ID, invocation.getPlatformDescriptor().getBomArtifactId());

        try (BuildFile buildFile = getBuildFile()) {
            String bomVersion = invocation.getPlatformDescriptor().getBomVersion();

            invocation.setProperty(BOM_VERSION, bomVersion);
            invocation.setProperty(QUARKUS_VERSION, invocation.getPlatformDescriptor().getQuarkusVersion());
            invocation.setValue(SOURCE_TYPE, sourceType);
            invocation.setValue(BUILD_FILE, buildFile);

            if (extensions != null && extensions.stream().anyMatch(e -> e.toLowerCase().contains("spring-web"))) {
                invocation.setValue(IS_SPRING, Boolean.TRUE);
            }

            if (className != null) {
                className = sourceType.stripExtensionFrom(className);
                int idx = className.lastIndexOf('.');
                if (idx >= 0) {
                    final String packageName = className.substring(0, idx);
                    className = className.substring(idx + 1);
                    invocation.setProperty(PACKAGE_NAME, packageName);
                }
                invocation.setProperty(CLASS_NAME, className);
            }

            ProjectGeneratorRegistry.get(BasicRestProjectGenerator.NAME).generate(writer, invocation);

            // call close at the end to save file
            buildFile.completeFile(groupId, artifactId, version, invocation.getPlatformDescriptor(), quarkusProps);
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to create project", e);
        }
        return QuarkusCommandOutcome.success();
    }

    private BuildFile getBuildFile() throws IOException {
        if (buildFile == null) {
            if (buildTool == null) {
                buildFile = new MavenBuildFile(writer);
            } else {
                buildFile = buildTool.createBuildFile(writer);
            }
        }
        return buildFile;
    }

    public static SourceType determineSourceType(Set<String> extensions) {
        Optional<SourceType> sourceType = extensions.stream()
                .map(SourceType::parse)
                .filter(Optional::isPresent)
                .map(e -> e.orElse(SourceType.JAVA))
                .findAny();
        return sourceType.orElse(SourceType.JAVA);
    }
}

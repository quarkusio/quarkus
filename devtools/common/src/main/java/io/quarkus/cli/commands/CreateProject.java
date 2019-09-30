package io.quarkus.cli.commands;

import static io.quarkus.generators.ProjectGenerator.BUILD_FILE;
import static io.quarkus.generators.ProjectGenerator.CLASS_NAME;
import static io.quarkus.generators.ProjectGenerator.IS_SPRING;
import static io.quarkus.generators.ProjectGenerator.PACKAGE_NAME;
import static io.quarkus.generators.ProjectGenerator.PROJECT_ARTIFACT_ID;
import static io.quarkus.generators.ProjectGenerator.PROJECT_GROUP_ID;
import static io.quarkus.generators.ProjectGenerator.PROJECT_VERSION;
import static io.quarkus.generators.ProjectGenerator.QUARKUS_VERSION;
import static io.quarkus.generators.ProjectGenerator.SOURCE_TYPE;
import static io.quarkus.maven.utilities.MojoUtils.getPluginVersion;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.model.Model;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.generators.ProjectGeneratorRegistry;
import io.quarkus.generators.SourceType;
import io.quarkus.generators.rest.BasicRestProjectGenerator;
import io.quarkus.maven.utilities.MojoUtils;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class CreateProject {

    private ProjectWriter writer;
    private String groupId;
    private String artifactId;
    private String version = getPluginVersion();
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
        if (!writer.init()) {
            return false;
        }

        MojoUtils.getAllProperties().forEach((k, v) -> context.put(k.replace("-", "_"), v));

        context.put(PROJECT_GROUP_ID, groupId);
        context.put(PROJECT_ARTIFACT_ID, artifactId);
        context.put(PROJECT_VERSION, version);
        context.put(QUARKUS_VERSION, getPluginVersion());
        context.put(SOURCE_TYPE, sourceType);
        context.put(BUILD_FILE, getBuildFile());

        if (extensions != null && extensions.stream().anyMatch(e -> e.toLowerCase().contains("spring-web"))) {
            context.put(IS_SPRING, Boolean.TRUE);
        }

        if (className != null) {
            className = sourceType.stripExtensionFrom(className);
            int idx = className.lastIndexOf('.');
            if (idx >= 0) {
                final String packageName = className.substring(0, idx);
                className = className.substring(idx + 1);
                context.put(PACKAGE_NAME, packageName);
            }
            context.put(CLASS_NAME, className);
        }

        ProjectGeneratorRegistry.get(BasicRestProjectGenerator.NAME).generate(writer, context);

        // call close at the end to save file
        try (BuildFile buildFile = getBuildFile()) {
            buildFile.completeFile(groupId, artifactId, version);
        }

        return true;
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

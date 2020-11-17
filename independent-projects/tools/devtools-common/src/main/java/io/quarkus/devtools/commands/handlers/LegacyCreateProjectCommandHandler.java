package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.computeCoordsFromQuery;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.*;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.codestarts.utils.NestedMaps;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.buildfile.GroovyGradleBuildFilesCreator;
import io.quarkus.devtools.project.buildfile.KotlinGradleBuildFilesCreator;
import io.quarkus.devtools.project.codegen.ProjectGenerator;
import io.quarkus.devtools.project.codegen.ProjectGeneratorRegistry;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.devtools.project.codegen.rest.BasicRestProjectGenerator;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ToolsUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Instances of this class are thread-safe. They create a new project extracting all the necessary properties from an instance
 * of {@link QuarkusCommandInvocation}.
 */
public class LegacyCreateProjectCommandHandler implements QuarkusCommandHandler {

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final QuarkusPlatformDescriptor platformDescr = invocation.getPlatformDescriptor();
        invocation.setValue(BOM_GROUP_ID, platformDescr.getBomGroupId());
        invocation.setValue(BOM_ARTIFACT_ID, platformDescr.getBomArtifactId());
        invocation.setValue(QUARKUS_VERSION, platformDescr.getQuarkusVersion());
        invocation.setValue(BOM_VERSION, platformDescr.getBomVersion());
        final Set<String> extensionsQuery = invocation.getValue(ProjectGenerator.EXTENSIONS, Collections.emptySet());

        final Properties quarkusProps = ToolsUtils.readQuarkusProperties(platformDescr);
        quarkusProps.forEach((k, v) -> {
            String name = k.toString().replace("-", "_");
            if (!invocation.hasValue(name)) {
                invocation.setValue(k.toString().replace("-", "_"), v.toString());
            }
        });

        addPlatformDataToLegacyCodegen(invocation);

        try {
            String className = invocation.getStringValue(CLASS_NAME);
            if (className != null) {
                className = invocation.getValue(SOURCE_TYPE, SourceType.JAVA).stripExtensionFrom(className);
                int idx = className.lastIndexOf('.');
                if (idx >= 0) {
                    String pkgName = invocation.getStringValue(PACKAGE_NAME);
                    if (pkgName == null) {
                        invocation.setValue(PACKAGE_NAME, className.substring(0, idx));
                    }
                    className = className.substring(idx + 1);
                }
                invocation.setValue(CLASS_NAME, className);
            }

            // Default to cleaned groupId if packageName not set
            final String pkgName = invocation.getStringValue(PACKAGE_NAME);
            final String groupId = invocation.getStringValue(PROJECT_GROUP_ID);
            if (pkgName == null && groupId != null) {
                invocation.setValue(PACKAGE_NAME, groupId.replace("-", ".").replace("_", "."));
            }

            final List<AppArtifactCoords> extensionsToAdd = computeCoordsFromQuery(invocation, extensionsQuery);

            // extensionsToAdd is null when an error occurred while matching extensions
            if (extensionsToAdd != null) {
                ProjectGeneratorRegistry.get(BasicRestProjectGenerator.NAME).generate(invocation);

                //TODO ia3andy extensions should be added directly during the project generation
                if (invocation.getQuarkusProject().getBuildTool().equals(BuildTool.GRADLE)) {
                    final GroovyGradleBuildFilesCreator generator = new GroovyGradleBuildFilesCreator(
                            invocation.getQuarkusProject());
                    generator.create(
                            invocation.getStringValue(PROJECT_GROUP_ID),
                            invocation.getStringValue(PROJECT_ARTIFACT_ID),
                            invocation.getStringValue(PROJECT_VERSION),
                            quarkusProps,
                            extensionsToAdd);
                } else if (invocation.getQuarkusProject().getBuildTool().equals(BuildTool.GRADLE_KOTLIN_DSL)) {
                    final KotlinGradleBuildFilesCreator generator = new KotlinGradleBuildFilesCreator(
                            invocation.getQuarkusProject());
                    generator.create(
                            invocation.getStringValue(PROJECT_GROUP_ID),
                            invocation.getStringValue(PROJECT_ARTIFACT_ID),
                            invocation.getStringValue(PROJECT_VERSION),
                            quarkusProps,
                            extensionsToAdd);
                } else {
                    final ExtensionManager.InstallResult result = invocation.getQuarkusProject().getExtensionManager()
                            .install(extensionsToAdd);
                    result.getInstalled()
                            .forEach(a -> invocation.log()
                                    .info(MessageIcons.OK_ICON + " Extension " + a.getGroupId() + ":" + a.getArtifactId()
                                            + " has been installed"));
                }
            }
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to create project", e);
        }
        return QuarkusCommandOutcome.success();
    }

    // # CLOSE YOUR EYES PLEASE
    static void addPlatformDataToLegacyCodegen(QuarkusCommandInvocation invocation) {
        final BuildTool buildTool = invocation.getQuarkusProject().getBuildTool();
        if (BuildTool.MAVEN == buildTool) {
            final Optional<List> mavenRepositories = NestedMaps.getValue(invocation.getPlatformDescriptor().getMetadata(),
                    "maven.repositories");
            if (mavenRepositories.isPresent()
                    && !mavenRepositories.get().isEmpty()) {
                // We only take the first one here to make things simpler:
                final Map<String, Object> repo = (Map<String, Object>) mavenRepositories.get().get(0);
                if (repo != null && repo.get("id") instanceof String && repo.get("url") instanceof String) {
                    final StringBuilder repositories = new StringBuilder()
                            .append("\n")
                            .append("    <repositories>\n")
                            .append("        <repository>\n")
                            .append("            <id>").append(repo.get("id")).append("</id>\n")
                            .append("            <url>").append(repo.get("url")).append("</url>\n")
                            .append("            <releases>\n")
                            .append("                <enabled>").append(repo.getOrDefault("releases-enabled", true))
                            .append("</enabled>\n")
                            .append("            </releases>\n")
                            .append("            <snapshots>\n")
                            .append("                <enabled>").append(repo.getOrDefault("snapshots-enabled", true))
                            .append("</enabled>\n")
                            .append("            </snapshots>\n")
                            .append("        </repository>\n")
                            .append("    </repositories>\n");
                    invocation.setValue(MAVEN_REPOSITORIES, repositories.toString());
                }
            }
            final Optional<List> mavenPluginRepositories = NestedMaps
                    .getValue(invocation.getPlatformDescriptor().getMetadata(), "maven.plugin-repositories");
            if (mavenPluginRepositories.isPresent()
                    && !mavenPluginRepositories.get().isEmpty()) {
                // We only take the first one here to make things simpler:
                final Map<String, Object> repo = (Map<String, Object>) mavenPluginRepositories.get().get(0);
                if (repo != null && repo.get("id") instanceof String && repo.get("url") instanceof String) {
                    final StringBuilder pluginRepositories = new StringBuilder()
                            .append("\n")
                            .append("    <pluginRepositories>\n")
                            .append("        <pluginRepository>\n")
                            .append("            <id>").append(repo.get("id")).append("</id>\n")
                            .append("            <url>").append(repo.get("url")).append("</url>\n")
                            .append("            <releases>\n")
                            .append("                <enabled>").append(repo.getOrDefault("releases-enabled", true))
                            .append("</enabled>\n")
                            .append("            </releases>\n")
                            .append("            <snapshots>\n")
                            .append("                <enabled>").append(repo.getOrDefault("snapshots-enabled", true))
                            .append("</enabled>\n")
                            .append("            </snapshots>\n")
                            .append("        </pluginRepository>\n")
                            .append("    </pluginRepositories>\n");
                    invocation.setValue(MAVEN_PLUGIN_REPOSITORIES, pluginRepositories.toString());
                }
            }
        } else if (BuildTool.GRADLE == buildTool || BuildTool.GRADLE_KOTLIN_DSL == buildTool) {
            final Optional<List> gradleRepositories = NestedMaps
                    .getValue(invocation.getPlatformDescriptor().getMetadata(), "gradle.repositories");
            if (gradleRepositories.isPresent()
                    && !gradleRepositories.get().isEmpty()) {
                // We only take the first one here to make things simpler:
                final Map<String, Object> repo = (Map<String, Object>) gradleRepositories.get().get(0);
                if (repo != null && repo.get("url") instanceof String) {
                    final String repositories = buildTool == BuildTool.GRADLE
                            ? "\n     maven { url \"" + repo.get("url") + "\" }"
                            : "\n     maven { url = uri(\"" + repo.get("url") + "\") }";
                    invocation.setValue(MAVEN_REPOSITORIES, repositories);
                }
            }
            final Optional<List> gradlePluginRepositories = NestedMaps
                    .getValue(invocation.getPlatformDescriptor().getMetadata(), "gradle.plugin-repositories");
            if (gradlePluginRepositories.isPresent()
                    && !gradlePluginRepositories.get().isEmpty()) {
                // We only take the first one here to make things simpler:
                final Map<String, Object> repo = (Map<String, Object>) gradlePluginRepositories.get().get(0);
                if (repo != null && repo.get("url") instanceof String) {
                    final String pluginRepositories = buildTool == BuildTool.GRADLE
                            ? "\n     maven { url \"" + repo.get("url") + "\" }"
                            : "\n     maven { url = uri(\"" + repo.get("url") + "\") }";
                    invocation.setValue(MAVEN_PLUGIN_REPOSITORIES, pluginRepositories);
                }
            }
        }
    }
    // # YOU CAN OPEN NOW :)
}

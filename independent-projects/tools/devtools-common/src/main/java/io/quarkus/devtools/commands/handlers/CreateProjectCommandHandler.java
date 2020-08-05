package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.computeCoordsFromQuery;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.*;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartData;
import io.quarkus.devtools.codestarts.CodestartInput;
import io.quarkus.devtools.codestarts.CodestartProject;
import io.quarkus.devtools.codestarts.CodestartSpec;
import io.quarkus.devtools.codestarts.Codestarts;
import io.quarkus.devtools.codestarts.QuarkusCodestarts;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.buildfile.GroovyGradleBuildFilesCreator;
import io.quarkus.devtools.project.buildfile.KotlinGradleBuildFilesCreator;
import io.quarkus.devtools.project.codegen.ProjectGenerator;
import io.quarkus.devtools.project.codegen.ProjectGeneratorRegistry;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.devtools.project.codegen.rest.BasicRestProjectGenerator;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ConsoleMessageFormats;
import io.quarkus.platform.tools.ToolsUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Instances of this class are thread-safe. They create a new project extracting all the necessary properties from an instance
 * of {@link QuarkusCommandInvocation}.
 */
public class CreateProjectCommandHandler implements QuarkusCommandHandler {

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

        if (invocation.getValue("codestarts.enabled", false)) {
            final List<AppArtifactKey> extensionsToAdd = computeCoordsFromQuery(invocation, extensionsQuery).stream()
                    .map(AppArtifactCoords::getKey)
                    .collect(Collectors.toList());

            try {
                final CodestartInput input = QuarkusCodestarts.inputBuilder(platformDescr)
                        .addExtensions(extensionsToAdd)
                        .addData(CodestartData.LegacySupport.convertFromLegacy(invocation.getValues()))
                        .putData(CodestartData.DataKey.BUILDTOOL.getKey(),
                                invocation.getQuarkusProject().getBuildTool().toString().toLowerCase().replace("_", "-"))
                        .includeExamples(invocation.getValue("codestarts.with-example-code", true))
                        .build();
                invocation.log().info("Generating Quarkus Codestart Project with data: " + input.getData().toString());
                final CodestartProject codestartProject = Codestarts
                        .prepareProject(input);
                invocation.log().info("Codestarts: " + codestartProject.getCodestarts().stream().map(Codestart::getSpec)
                        .map(CodestartSpec::getName).collect(Collectors.joining(", ")));
                Codestarts.generateProject(codestartProject, invocation.getQuarkusProject().getProjectDirPath());
            } catch (IOException e) {
                throw new QuarkusCommandException("Failed to create project", e);
            }
            return QuarkusCommandOutcome.success();
        }

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
                                    .info(ConsoleMessageFormats.ok("Extension " + a.getGroupId() + ":" + a.getArtifactId())
                                            + " has been installed"));
                }
            }
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to create project", e);
        }
        return QuarkusCommandOutcome.success();
    }
}

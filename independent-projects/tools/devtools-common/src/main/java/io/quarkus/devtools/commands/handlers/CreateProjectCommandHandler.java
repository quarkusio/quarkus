package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.CreateProject.CODESTARTS;
import static io.quarkus.devtools.commands.CreateProject.NO_BUILDTOOL_WRAPPER;
import static io.quarkus.devtools.commands.CreateProject.NO_DOCKERFILES;
import static io.quarkus.devtools.commands.CreateProject.NO_EXAMPLES;
import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.computeCoordsFromQuery;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.BOM_ARTIFACT_ID;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.BOM_GROUP_ID;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.BOM_VERSION;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.CLASS_NAME;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PACKAGE_NAME;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PROJECT_GROUP_ID;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.QUARKUS_VERSION;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.codestarts.CodestartProjectDefinition;
import io.quarkus.devtools.codestarts.CodestartType;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.LegacySupport;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartProjectInput;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.project.codegen.ProjectGenerator;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ToolsUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

        // Default to cleaned groupId if packageName not set
        final String className = invocation.getStringValue(CLASS_NAME);
        final String pkgName = invocation.getStringValue(PACKAGE_NAME);
        final String groupId = invocation.getStringValue(PROJECT_GROUP_ID);
        if (pkgName == null) {
            if (className != null && className.contains(".")) {
                final int idx = className.lastIndexOf('.');
                invocation.setValue(PACKAGE_NAME, className.substring(0, idx));
                invocation.setValue(CLASS_NAME, className.substring(idx + 1));
            } else if (groupId != null) {
                invocation.setValue(PACKAGE_NAME, groupId.replace("-", ".").replace("_", "."));
            }
        }

        final List<AppArtifactCoords> extensionsToAdd = computeCoordsFromQuery(invocation, extensionsQuery);
        if (extensionsToAdd == null) {
            throw new QuarkusCommandException("Failed to create project because of invalid extensions");
        }
        try {
            Map<String, Object> platformData = new HashMap<>();
            if (platformDescr.getMetadata().get("maven") != null) {
                platformData.put("maven", platformDescr.getMetadata().get("maven"));
            }
            if (platformDescr.getMetadata().get("gradle") != null) {
                platformData.put("gradle", platformDescr.getMetadata().get("gradle"));
            }
            final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                    .addExtensions(extensionsToAdd)
                    .buildTool(invocation.getQuarkusProject().getBuildTool())
                    .addCodestarts(invocation.getValue(CODESTARTS, new HashSet<>()))
                    .noExamples(invocation.getValue(NO_EXAMPLES, false))
                    .noBuildToolWrapper(invocation.getValue(NO_BUILDTOOL_WRAPPER, false))
                    .noDockerfiles(invocation.getValue(NO_DOCKERFILES, false))
                    .addData(platformData)
                    .addData(LegacySupport.convertFromLegacy(invocation.getValues()))
                    .messageWriter(invocation.log())
                    .build();
            invocation.log().info("-----------");
            if (!extensionsToAdd.isEmpty()) {
                invocation.log().info("selected extensions: \n"
                        + extensionsToAdd.stream().map(e -> "- " + e.getGroupId() + ":" + e.getArtifactId() + "\n")
                                .collect(Collectors.joining()));
            }

            final QuarkusCodestartCatalog catalog = QuarkusCodestartCatalog
                    .fromQuarkusPlatformDescriptor(invocation.getPlatformDescriptor());
            final CodestartProjectDefinition projectDefinition = catalog.createProject(input);
            projectDefinition.generate(invocation.getQuarkusProject().getProjectDirPath());
            invocation.log()
                    .info("\n-----------\n" + MessageIcons.NOOP_ICON + " "
                            + projectDefinition.getRequiredCodestart(CodestartType.PROJECT).getName()
                            + " project has been successfully generated in:\n--> "
                            + invocation.getQuarkusProject().getProjectDirPath().toString() + "\n-----------");

        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to create project", e);
        }
        return QuarkusCommandOutcome.success();
    }
}

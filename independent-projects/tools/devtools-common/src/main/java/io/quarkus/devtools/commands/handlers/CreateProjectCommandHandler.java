package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.APP_CONFIG;
import static io.quarkus.devtools.commands.CreateProject.EXAMPLE;
import static io.quarkus.devtools.commands.CreateProject.EXTRA_CODESTARTS;
import static io.quarkus.devtools.commands.CreateProject.NO_BUILDTOOL_WRAPPER;
import static io.quarkus.devtools.commands.CreateProject.NO_CODE;
import static io.quarkus.devtools.commands.CreateProject.NO_DOCKERFILES;
import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.computeCoordsFromQuery;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.BOM_ARTIFACT_ID;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.BOM_GROUP_ID;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.BOM_VERSION;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.CLASS_NAME;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PACKAGE_NAME;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.PROJECT_GROUP_ID;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.QUARKUS_VERSION;

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
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.union.ElementCatalog;
import io.quarkus.registry.union.ElementCatalogBuilder;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
        final ExtensionCatalog extensionCatalog = invocation.getExtensionsCatalog();
        final Set<String> extensionsQuery = invocation.getValue(ProjectGenerator.EXTENSIONS, Collections.emptySet());

        final Properties quarkusProps = ToolsUtils.readQuarkusProperties(extensionCatalog);
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

        final List<ArtifactCoords> extensionsToAdd = computeCoordsFromQuery(invocation, extensionsQuery);
        if (extensionsToAdd == null) {
            throw new QuarkusCommandException("Failed to create project because of invalid extensions");
        }

        ArtifactCoords bom = null;
        final List<ArtifactCoords> boms = getExtensionBoms(extensionCatalog, extensionsToAdd);
        if (boms.isEmpty()) {
            bom = extensionCatalog.getBom();
        } else {
            final Iterator<ArtifactCoords> i = boms.iterator();
            while (i.hasNext()) {
                final ArtifactCoords next = i.next();
                // TODO we remove quarkus-bom here because it's currently added by default using properties in the template
                // ideally, it shouldn't be different than the rest of the imported BOMs
                if (next.getArtifactId().equals("quarkus-bom")) {
                    bom = next;
                    i.remove();
                    break;
                }
            }
        }

        if (bom == null) {
            throw new QuarkusCommandException("The platform BOM is missing");
        }

        invocation.setValue(BOM_GROUP_ID, bom.getGroupId());
        invocation.setValue(BOM_ARTIFACT_ID, bom.getArtifactId());
        invocation.setValue(BOM_VERSION, bom.getVersion());
        invocation.setValue(QUARKUS_VERSION, extensionCatalog.getQuarkusCoreVersion());

        try {
            Map<String, Object> platformData = new HashMap<>();
            if (extensionCatalog.getMetadata().get("maven") != null) {
                platformData.put("maven", extensionCatalog.getMetadata().get("maven"));
            }
            if (extensionCatalog.getMetadata().get("gradle") != null) {
                platformData.put("gradle", extensionCatalog.getMetadata().get("gradle"));
            }
            final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                    .addPlatforms(boms)
                    .addExtensions(extensionsToAdd)
                    .buildTool(invocation.getQuarkusProject().getBuildTool())
                    .example(invocation.getValue(EXAMPLE))
                    .noCode(invocation.getValue(NO_CODE, false))
                    .addCodestarts(invocation.getValue(EXTRA_CODESTARTS, Collections.emptySet()))
                    .noBuildToolWrapper(invocation.getValue(NO_BUILDTOOL_WRAPPER, false))
                    .noDockerfiles(invocation.getValue(NO_DOCKERFILES, false))
                    .addData(platformData)
                    .addData(LegacySupport.convertFromLegacy(invocation.getValues()))
                    .putData(APP_CONFIG.key(), invocation.getValue(APP_CONFIG.key(), Collections.emptyMap()))
                    .messageWriter(invocation.log())
                    .build();
            invocation.log().info("-----------");
            if (!extensionsToAdd.isEmpty()) {
                invocation.log().info("selected extensions: \n"
                        + extensionsToAdd.stream().map(e -> "- " + e.getGroupId() + ":" + e.getArtifactId() + "\n")
                                .collect(Collectors.joining()));
            }

            final QuarkusCodestartCatalog catalog = QuarkusCodestartCatalog
                    .fromExtensionsCatalog(invocation.getQuarkusProject().getExtensionsCatalog(),
                            invocation.getQuarkusProject().getCodestartResourceLoaders());
            final CodestartProjectDefinition projectDefinition = catalog.createProject(input);
            projectDefinition.generate(invocation.getQuarkusProject().getProjectDirPath());
            invocation.log()
                    .info("\n-----------\n" + MessageIcons.OK_ICON + " "
                            + projectDefinition.getRequiredCodestart(CodestartType.PROJECT).getName()
                            + " project has been successfully generated in:\n--> "
                            + invocation.getQuarkusProject().getProjectDirPath().toString() + "\n-----------");

        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to create project: " + e.getMessage(), e);
        }

        return QuarkusCommandOutcome.success();
    }

    private List<ArtifactCoords> getExtensionBoms(ExtensionCatalog extensionCatalog, List<ArtifactCoords> extensionsToAdd)
            throws QuarkusCommandException {
        final ElementCatalog ec = (ElementCatalog) extensionCatalog.getMetadata().get("element-catalog");
        if (ec == null) {
            return Collections.emptyList();
        }
        // we add quarkus-core as a selected extension here only to include the quarkus-bom
        // in the list of platforms. quarkus-core won't be added to the generated POM though.
        final Extension quarkusCore = extensionCatalog.getExtensions().stream()
                .filter(e -> e.getArtifact().getArtifactId().equals("quarkus-core")).findFirst().get();
        if (quarkusCore == null) {
            throw new QuarkusCommandException("Failed to locate quarkus-core in the extension catalog");
        }
        final List<String> eKeys;
        if (extensionsToAdd.isEmpty()) {
            eKeys = Collections.singletonList(
                    quarkusCore.getArtifact().getGroupId() + ":" + quarkusCore.getArtifact().getArtifactId());
        } else {
            eKeys = extensionsToAdd.stream().map(e -> e.getGroupId() + ":" + e.getArtifactId())
                    .collect(Collectors.toList());
            eKeys.add(quarkusCore.getArtifact().getGroupId() + ":" + quarkusCore.getArtifact().getArtifactId());
        }
        return ElementCatalogBuilder.getBoms(ec, eKeys);
    }
}

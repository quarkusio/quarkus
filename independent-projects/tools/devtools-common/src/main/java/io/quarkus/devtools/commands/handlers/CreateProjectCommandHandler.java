package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.CreateProject.EXAMPLE;
import static io.quarkus.devtools.commands.CreateProject.EXTRA_CODESTARTS;
import static io.quarkus.devtools.commands.CreateProject.NO_BUILDTOOL_WRAPPER;
import static io.quarkus.devtools.commands.CreateProject.NO_CODE;
import static io.quarkus.devtools.commands.CreateProject.NO_DOCKERFILES;
import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.computeExtensionsFromQuery;
import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.devtools.project.codegen.ProjectGenerator.APP_CONFIG;
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
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.codegen.ProjectGenerator;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.json.JsonCatalogMerger;
import io.quarkus.registry.union.ElementCatalog;
import io.quarkus.registry.union.ElementCatalogBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        final Set<String> extensionsQuery = invocation.getValue(ProjectGenerator.EXTENSIONS, Collections.emptySet());

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
                invocation.setValue(PACKAGE_NAME, groupId.replace('-', '.').replace('_', '.'));
            }
        }

        List<Extension> extensionsToAdd = computeRequiredExtensions(invocation.getExtensionsCatalog(), extensionsQuery,
                invocation.log());

        ExtensionCatalog mainCatalog = invocation.getExtensionsCatalog(); // legacy platform initialization
        final List<ExtensionCatalog> extensionOrigins = getExtensionOrigins(mainCatalog, extensionsToAdd);
        final List<ArtifactCoords> platformBoms = new ArrayList<>(Math.max(extensionOrigins.size(), 1));
        if (extensionOrigins.size() > 0) {
            // necessary to set the versions from the selected origins
            extensionsToAdd = computeRequiredExtensions(JsonCatalogMerger.merge(extensionOrigins), extensionsQuery,
                    invocation.log());
            // collect platform BOMs to import
            boolean sawFirstPlatform = false;
            for (ExtensionCatalog c : extensionOrigins) {
                if (!c.isPlatform()) {
                    continue;
                }
                if (c.getBom().getArtifactId().equals("quarkus-bom") || !sawFirstPlatform) {
                    mainCatalog = c;
                    sawFirstPlatform = true;
                }
                platformBoms.add(c.getBom());
            }
        } else if (ElementCatalogBuilder.hasElementCatalog(mainCatalog)) {
            final StringBuilder buf = new StringBuilder();
            buf.append(ERROR_ICON);
            buf.append(" Failed to determine a compatible Quarkus version for the requested extensions: ");
            buf.append(extensionsToAdd.get(0).getArtifact().getKey().toGacString());
            for (int i = 1; i < extensionsToAdd.size(); ++i) {
                buf.append(", ").append(extensionsToAdd.get(i).getArtifact().getKey().toGacString());
            }
            invocation.log().info(buf.toString());
            return QuarkusCommandOutcome.failure();
        } else {
            platformBoms.add(mainCatalog.getBom());
        }

        final List<ArtifactCoords> extensionCoords = new ArrayList<>(extensionsToAdd.size());
        for (Extension e : extensionsToAdd) {
            ArtifactCoords coords = e.getArtifact();
            for (ExtensionOrigin origin : e.getOrigins()) {
                if (origin.isPlatform() && origin.getBom() != null && platformBoms.contains(origin.getBom())) {
                    coords = Extensions.stripVersion(coords);
                    break;
                }
            }
            extensionCoords.add(coords);
        }

        invocation.setValue(BOM_GROUP_ID, mainCatalog.getBom().getGroupId());
        invocation.setValue(BOM_ARTIFACT_ID, mainCatalog.getBom().getArtifactId());
        invocation.setValue(BOM_VERSION, mainCatalog.getBom().getVersion());
        invocation.setValue(QUARKUS_VERSION, mainCatalog.getQuarkusCoreVersion());
        final Properties quarkusProps = ToolsUtils.readQuarkusProperties(mainCatalog);
        quarkusProps.forEach((k, v) -> {
            final String name = k.toString().replace('-', '_');
            if (!invocation.hasValue(name)) {
                invocation.setValue(name, v.toString());
            }
        });

        try {
            Map<String, Object> platformData = new HashMap<>();
            if (mainCatalog.getMetadata().get("maven") != null) {
                platformData.put("maven", mainCatalog.getMetadata().get("maven"));
            }
            if (mainCatalog.getMetadata().get("gradle") != null) {
                platformData.put("gradle", mainCatalog.getMetadata().get("gradle"));
            }
            final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                    .addPlatforms(platformBoms)
                    .addExtensions(extensionCoords)
                    .buildTool(invocation.getQuarkusProject().getBuildTool())
                    .example(invocation.getValue(EXAMPLE))
                    .noCode(invocation.getValue(NO_CODE, false))
                    .addCodestarts(invocation.getValue(EXTRA_CODESTARTS, Collections.emptySet()))
                    .noBuildToolWrapper(invocation.getValue(NO_BUILDTOOL_WRAPPER, false))
                    .noDockerfiles(invocation.getValue(NO_DOCKERFILES, false))
                    .addData(platformData)
                    .addData(LegacySupport.convertFromLegacy(invocation.getValues()))
                    .putData(APP_CONFIG, invocation.getValue(APP_CONFIG, Collections.emptyMap()))
                    .messageWriter(invocation.log())
                    .build();
            invocation.log().info("-----------");
            if (!extensionsToAdd.isEmpty()) {
                invocation.log().info("selected extensions: \n"
                        + extensionsToAdd.stream()
                                .map(e -> "- " + e.getArtifact().getGroupId() + ":" + e.getArtifact().getArtifactId() + "\n")
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

    private List<Extension> computeRequiredExtensions(ExtensionCatalog catalog,
            final Set<String> extensionsQuery, MessageWriter log) throws QuarkusCommandException {
        final List<Extension> extensionsToAdd = computeExtensionsFromQuery(catalog, extensionsQuery, log);
        if (extensionsToAdd == null) {
            throw new QuarkusCommandException("Failed to create project because of invalid extensions");
        }
        return extensionsToAdd;
    }

    private List<ExtensionCatalog> getExtensionOrigins(ExtensionCatalog extensionCatalog, List<Extension> extensionsToAdd)
            throws QuarkusCommandException {
        final ElementCatalog<ExtensionCatalog> ec = ElementCatalogBuilder.getElementCatalog(extensionCatalog,
                ExtensionCatalog.class);
        if (ec == null) {
            return Collections.emptyList();
        }
        // we add quarkus-core as a selected extension here only to include the quarkus-bom
        // in the list of platforms. quarkus-core won't be added to the generated POM though.
        final Optional<Extension> quarkusCore = extensionCatalog.getExtensions().stream()
                .filter(e -> e.getArtifact().getArtifactId().equals("quarkus-core")).findFirst();
        if (!quarkusCore.isPresent()) {
            throw new QuarkusCommandException("Failed to locate quarkus-core in the extension catalog");
        }
        final ArtifactCoords quarkusCoreCoords = quarkusCore.get().getArtifact();
        final List<String> eKeys;
        if (extensionsToAdd.isEmpty()) {
            eKeys = Collections.singletonList(
                    quarkusCoreCoords.getGroupId() + ":" + quarkusCoreCoords.getArtifactId());
        } else {
            eKeys = new ArrayList<>(extensionsToAdd.size() + 1);
            eKeys.add(quarkusCoreCoords.getGroupId() + ":" + quarkusCoreCoords.getArtifactId());
            extensionsToAdd.forEach(e -> eKeys.add(e.getArtifact().getGroupId() + ":" + e.getArtifact().getArtifactId()));
        }
        return ElementCatalogBuilder.getMembersForElements(ec, eKeys);
    }
}

package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.CreateProject.CreateProjectKey.*;
import static io.quarkus.devtools.commands.handlers.CreateProjectCodestartDataConverter.toCodestartData;
import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.computeExtensionsFromQuery;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getMinimumJavaVersion;
import static io.quarkus.platform.tools.ToolsUtils.countOf;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.devtools.codestarts.CodestartProjectDefinition;
import io.quarkus.devtools.codestarts.CodestartType;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartProjectInput;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.CreateProjectCodestartDataConverter.CatalogKey;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.JavaVersion;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.CatalogMergeUtility;
import io.quarkus.registry.Constants;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.selection.ExtensionOrigins;
import io.quarkus.registry.catalog.selection.OriginCombination;
import io.quarkus.registry.catalog.selection.OriginPreference;
import io.quarkus.registry.catalog.selection.OriginSelector;

/**
 * Instances of this class are thread-safe. They create a new project extracting all the necessary properties from an instance
 * of {@link QuarkusCommandInvocation}.
 */
public class CreateProjectCommandHandler implements QuarkusCommandHandler {

    private static final String MAVEN = "maven";
    private static final String GRADLE = "gradle";
    private static final String QUARKUS_BOM = "quarkus-bom";
    private static final String QUARKUS_CORE = "quarkus-core";

    private static class ProjectDependencyInfo {

        private final ExtensionCatalog primaryCatalog;
        private final ExtensionCatalog dataCatalog;
        private final List<ArtifactCoords> platformBoms;
        private final List<ArtifactCoords> extensionDeps;

        ProjectDependencyInfo(ExtensionCatalog primaryCatalog, List<Extension> selectedExtensions) {

            this.primaryCatalog = primaryCatalog;
            this.dataCatalog = primaryCatalog;
            this.platformBoms = List.of(primaryCatalog.getBom());

            this.extensionDeps = new ArrayList<>(selectedExtensions.size());
            for (Extension e : selectedExtensions) {
                ArtifactCoords coords = e.getArtifact();
                for (ExtensionOrigin origin : e.getOrigins()) {
                    if (origin.isPlatform() && origin.getBom() != null && platformBoms.contains(origin.getBom())) {
                        coords = Extensions.stripVersion(coords);
                        break;
                    }
                }
                extensionDeps.add(coords);
            }

        }

        ProjectDependencyInfo(List<ExtensionCatalog> extensionOrigins, List<Extension> selectedExtensions,
                Collection<String> extensionsQuery) {

            final Map<ArtifactKey, ArtifactCoords> extensionDeps = new LinkedHashMap<>(selectedExtensions.size());

            // TODO Compatibility of user-provided extension artifacts to be taken into account.
            // If a user provided some complete artifact coordinates then we actually do not check for compatibility
            // of those artifact (possibly extensions) with the recommended platforms and Quarkus core.
            // It has worked like this from the beginning but it should be reviewed in the future.
            final Map<ArtifactKey, ArtifactCoords> userProvidedCoords = collectArtifactCoords(extensionsQuery);

            final ArrayDeque<Extension> extensionDeque = new ArrayDeque<>(selectedExtensions);

            // necessary to set the versions from the selected origins
            this.dataCatalog = CatalogMergeUtility.merge(extensionOrigins);
            // collect platform BOMs to import
            ExtensionCatalog primaryCatalog = null;
            this.platformBoms = new ArrayList<>(extensionOrigins.size());
            for (ExtensionCatalog c : extensionOrigins) {
                if (c.isPlatform()) {
                    // use either the first platform catalog or the first quarkus-bom catalog, if found
                    if (primaryCatalog == null
                            || !primaryCatalog.getBom().getArtifactId().equals(QUARKUS_BOM)
                                    && c.getBom().getArtifactId().equals(QUARKUS_BOM)) {
                        primaryCatalog = c;
                    }
                    platformBoms.add(c.getBom());
                }

                final Map<ArtifactKey, ArtifactCoords> allCatalogExtensions = getCatalogExtensions(c);
                var i = extensionDeque.iterator();
                while (i.hasNext()) {
                    final ArtifactKey extKey = i.next().getArtifact().getKey();
                    ArtifactCoords addedCoords = userProvidedCoords.get(extKey);
                    if (addedCoords != null) {
                        extensionDeps.put(extKey, addedCoords);
                        i.remove();
                    } else {
                        ArtifactCoords mappedCoords = extensionDeps.get(extKey);
                        if (mappedCoords == null || c.isPlatform()) {
                            addedCoords = allCatalogExtensions.get(extKey);
                            if (addedCoords != null) {
                                if (c.isPlatform()) {
                                    addedCoords = Extensions.stripVersion(addedCoords);
                                    i.remove();
                                }
                                extensionDeps.put(extKey, addedCoords);
                            }
                        }
                    }
                }
            }

            this.primaryCatalog = Objects.requireNonNull(primaryCatalog, "primary catalog is null");
            this.extensionDeps = List.copyOf(extensionDeps.values());
        }

        @SuppressWarnings("unchecked")
        private static Map<ArtifactKey, ArtifactCoords> getCatalogExtensions(ExtensionCatalog catalog) {
            if (catalog.getMetadata()
                    .get(Constants.REGISTRY_CLIENT_ALL_CATALOG_EXTENSIONS) instanceof Map<?, ?> allCatalogExtensions) {
                return (Map<ArtifactKey, ArtifactCoords>) allCatalogExtensions;
            }
            final Collection<Extension> extensions = catalog.getExtensions();
            final Map<ArtifactKey, ArtifactCoords> result = new HashMap<>(extensions.size());
            for (Extension e : extensions) {
                result.put(e.getArtifact().getKey(), e.getArtifact());
            }
            return result;
        }

        /**
         * Collects complete artifact coordinates from a user-provided input. If a user-provided input
         * does not appear to contain complete artifact coordinates, the method returns an empty map.
         *
         * @param extensionsQuery user-provided input
         * @return complete artifact coordinates collected from a user-provided input
         */
        private static Map<ArtifactKey, ArtifactCoords> collectArtifactCoords(Collection<String> extensionsQuery) {
            Map<ArtifactKey, ArtifactCoords> result = Map.of();
            for (String extensionExpr : extensionsQuery) {
                if (countOf(extensionExpr, ':') > 1) {
                    if (result.isEmpty()) {
                        result = new HashMap<>();
                    }
                    final ArtifactCoords coords = ArtifactCoords.fromString(extensionExpr);
                    result.put(coords.getKey(), coords);
                }
            }
            return result;
        }

        ExtensionCatalog getPrimaryPlatformCatalog() {
            return primaryCatalog;
        }

        List<ArtifactCoords> getBoms() {
            return platformBoms;
        }

        List<ArtifactCoords> getDependencies() {
            return extensionDeps;
        }

        Map<String, Object> getPlatformProjectData() {
            return ToolsUtils.readProjectData(dataCatalog);
        }

        void setQuarkusProperties(QuarkusCommandInvocation invocation) {
            CreateProjectCommandHandler.setQuarkusProperties(invocation, dataCatalog);
        }
    }

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final Set<String> extensionsQuery = invocation.getValue(EXTENSIONS, Set.of());

        // Default to cleaned groupId if packageName not set
        final String className = invocation.getStringValue(RESOURCE_CLASS_NAME);
        final String pkgName = invocation.getStringValue(PACKAGE_NAME);
        final String groupId = invocation.getStringValue(PROJECT_GROUP_ID);
        if (pkgName == null) {
            if (className != null && className.contains(".")) {
                final int idx = className.lastIndexOf('.');
                invocation.setValue(PACKAGE_NAME, className.substring(0, idx));
                invocation.setValue(RESOURCE_CLASS_NAME, className.substring(idx + 1));
            } else if (groupId != null) {
                invocation.setValue(PACKAGE_NAME, groupId.replace('-', '.').replace('_', '.'));
            }
        }

        List<Extension> extensionsToAdd = computeRequiredExtensions(invocation.getExtensionsCatalog(), extensionsQuery,
                invocation.log());
        ExtensionCatalog mainCatalog = invocation.getExtensionsCatalog(); // legacy platform initialization

        final String javaVersion = invocation.getStringValue(JAVA_VERSION);
        checkMinimumJavaVersion(javaVersion, extensionsToAdd);
        final List<ExtensionCatalog> extensionOrigins = getExtensionOrigins(mainCatalog, extensionsToAdd);

        final ProjectDependencyInfo depInfo = extensionOrigins.isEmpty()
                ? new ProjectDependencyInfo(mainCatalog, extensionsToAdd)
                : new ProjectDependencyInfo(extensionOrigins, extensionsToAdd, extensionsQuery);
        mainCatalog = depInfo.getPrimaryPlatformCatalog();
        depInfo.setQuarkusProperties(invocation);

        invocation.setValue(CatalogKey.BOM_GROUP_ID, mainCatalog.getBom().getGroupId());
        invocation.setValue(CatalogKey.BOM_ARTIFACT_ID, mainCatalog.getBom().getArtifactId());
        invocation.setValue(CatalogKey.BOM_VERSION, mainCatalog.getBom().getVersion());
        invocation.setValue(QUARKUS_VERSION, mainCatalog.getQuarkusCoreVersion());

        try {
            Map<String, Object> platformData = new HashMap<>();
            if (mainCatalog.getMetadata().get(MAVEN) != null) {
                platformData.put(MAVEN, mainCatalog.getMetadata().get(MAVEN));
            }
            if (mainCatalog.getMetadata().get(GRADLE) != null) {
                platformData.put(GRADLE, mainCatalog.getMetadata().get(GRADLE));
            }
            final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                    .addPlatforms(depInfo.getBoms())
                    .addExtensions(depInfo.getDependencies())
                    .buildTool(invocation.getQuarkusProject().getBuildTool())
                    .example(invocation.getValue(EXAMPLE))
                    .noCode(invocation.getValue(NO_CODE, false))
                    .addCodestarts(invocation.getValue(EXTRA_CODESTARTS, Set.of()))
                    .noBuildToolWrapper(invocation.getValue(NO_BUILDTOOL_WRAPPER, false))
                    .noDockerfiles(invocation.getValue(NO_DOCKERFILES, false))
                    .addData(depInfo.getPlatformProjectData())
                    .addData(platformData)
                    .addData(toCodestartData(invocation.getValues()))
                    .addData(invocation.getValue(DATA, Map.of()))
                    .messageWriter(invocation.log())
                    .defaultCodestart(getDefaultCodestart(mainCatalog))
                    .build();

            invocation.log().info("-----------");
            if (!depInfo.getDependencies().isEmpty()) {
                invocation.log().info("selected extensions: \n"
                        + depInfo.getDependencies().stream()
                                .map(e -> "- " + e.getGroupId() + ":" + e.getArtifactId() + "\n")
                                .collect(Collectors.joining()));
            }

            final QuarkusCodestartCatalog catalog = QuarkusCodestartCatalog
                    .fromExtensionsCatalog(invocation.getQuarkusProject().getExtensionsCatalog(),
                            invocation.getQuarkusProject().getCodestartResourceLoaders());
            final CodestartProjectDefinition projectDefinition = catalog.createProject(input);
            projectDefinition.generate(invocation.getQuarkusProject().getProjectDirPath());
            invocation.log()
                    .info("\n-----------\n" + MessageIcons.SUCCESS_ICON + " "
                            + projectDefinition.getRequiredCodestart(CodestartType.PROJECT).getName()
                            + " project has been successfully generated in:\n--> "
                            + invocation.getQuarkusProject().getProjectDirPath().toString() + "\n-----------");

        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to create project: " + e.getMessage(), e);
        }

        return QuarkusCommandOutcome.success();
    }

    private static void setQuarkusProperties(QuarkusCommandInvocation invocation, ExtensionCatalog catalog) {
        var quarkusProps = ToolsUtils.readQuarkusProperties(catalog);
        quarkusProps.forEach((k, v) -> {
            final String name = k.toString();
            if (!invocation.hasValue(name)) {
                invocation.setValue(name, v.toString());
            }
        });
    }

    private List<Extension> computeRequiredExtensions(ExtensionCatalog catalog,
            final Set<String> extensionsQuery, MessageWriter log) throws QuarkusCommandException {
        final List<Extension> extensionsToAdd = computeExtensionsFromQuery(catalog, extensionsQuery, log);
        if (extensionsToAdd == null) {
            throw new QuarkusCommandException("Failed to create project because of invalid extensions");
        }
        return extensionsToAdd;
    }

    private static List<ExtensionCatalog> getExtensionOrigins(ExtensionCatalog extensionCatalog,
            List<Extension> extensionsToAdd)
            throws QuarkusCommandException {

        if (extensionsToAdd.isEmpty() && extensionCatalog.isPlatform()) {
            return List.of(extensionCatalog);
        }

        // we add quarkus-core as a selected extension here only to include the quarkus-bom
        // in the list of platforms. quarkus-core won't be added to the generated POM though.
        final Extension quarkusCore = findQuarkusCore(extensionCatalog);
        final List<ExtensionOrigins> originsWithPreferences;
        if (extensionsToAdd.isEmpty()) {
            // if no extensions were requested, we select the core BOM
            if (quarkusCore.getOrigins().size() == 1
                    && quarkusCore.getOrigins().get(0) instanceof ExtensionCatalog originCatalog) {
                // in this case, there is only one origin to choose from
                return List.of(originCatalog);
            }
            extensionsToAdd = List.of(quarkusCore);
        }

        originsWithPreferences = new ArrayList<>();
        for (Extension e : extensionsToAdd) {
            addOriginsWithPreferences(originsWithPreferences, e);
        }
        if (!originsWithPreferences.isEmpty()) {
            return getRecommendedOrigins(extensionsToAdd, quarkusCore, originsWithPreferences);
        }

        // no origin preferences were found (origin-preference data is missing)
        // this will happen if the extension catalog wasn't provided by a registry client
        if (extensionCatalog.isPlatform()) {
            // if the original catalog is platform, it should cover all the requested extensions
            return List.of(extensionCatalog);
        }

        // fallback to the best guess
        final Map<String, ExtensionCatalog> catalogMap = new LinkedHashMap<>();
        for (var e : extensionsToAdd) {
            final List<ExtensionOrigin> origins = e.getOrigins();
            if (!origins.isEmpty()) {
                if (origins.get(0) instanceof ExtensionCatalog originCatalog) {
                    catalogMap.putIfAbsent(originCatalog.getId(), originCatalog);
                }
            }
        }
        return List.copyOf(catalogMap.values());
    }

    private static List<ExtensionCatalog> getRecommendedOrigins(List<Extension> extensionsToAdd,
            Extension quarkusCore, List<ExtensionOrigins> extOrigins) throws QuarkusCommandException {
        final OriginCombination recommendedCombination = OriginSelector.of(extOrigins).calculateRecommendedCombination();
        if (recommendedCombination == null) {
            throw new QuarkusCommandException(noCompatibleQuarkusCoreVersion(extensionsToAdd));
        }
        // get the recommended catalogs in the right order according to the preferences
        List<ExtensionCatalog> sortedCatalogs = recommendedCombination.getUniqueSortedCatalogs();

        if (!containsQuarkusCore(extensionsToAdd)) {
            sortedCatalogs = ensureQuarkusCorePresent(sortedCatalogs, quarkusCore);
            if (sortedCatalogs.isEmpty()) {
                throw new QuarkusCommandException(noCompatibleQuarkusCoreVersion(extensionsToAdd));
            }
        }

        // with the introduction of offering-based filtering, some extensions may appear to be picked
        // from a catalog with a lower preference while still having their version managed in a catalog
        // with a higher preference.
        // For example, an unsupported extension managed in a downstream quarkus-bom could have been picked
        // from the upstream quarkus-bom. In this case we don't want to import both downstream and upstream
        // quarkus-boms. The logic below is making sure there is no unnecessary BOM overlap in such cases.
        if (sortedCatalogs.size() < 2) {
            return sortedCatalogs;
        }

        final List<ArtifactKey> extDeps = new ArrayList<>(extensionsToAdd.size());
        for (Extension e : extensionsToAdd) {
            extDeps.add(e.getArtifact().getKey());
        }
        List<ExtensionCatalog> reducedCatalogs = null;
        for (int i = 0; i < sortedCatalogs.size(); ++i) {
            var catalog = sortedCatalogs.get(i);
            if (reducedCatalogs != null) {
                reducedCatalogs.add(catalog);
            }
            if (catalog.isPlatform()) {
                var o = catalog.getMetadata().get(Constants.REGISTRY_CLIENT_ALL_CATALOG_EXTENSIONS);
                if (o instanceof Map<?, ?> allManagedExtensionKeys) {
                    if (extDeps.removeIf(allManagedExtensionKeys::containsKey)) {
                        if (reducedCatalogs == null) {
                            // if we got to the end, then we return the original catalogs
                            if (i == sortedCatalogs.size() - 1) {
                                break;
                            }
                            reducedCatalogs = new ArrayList<>(sortedCatalogs.size());
                            for (int j = 0; j <= i; ++j) {
                                reducedCatalogs.add(sortedCatalogs.get(j));
                            }
                        }
                        if (extDeps.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }
        return reducedCatalogs == null ? sortedCatalogs : reducedCatalogs;
    }

    private static List<ExtensionCatalog> ensureQuarkusCorePresent(List<ExtensionCatalog> catalogs, Extension quarkusCore) {
        Map<String, ExtensionCatalog> allCoreOrigins = null;
        ExtensionCatalog matchingQuarkusCoreOrigin = null;
        int catalogContainingCoreIndex = -1;
        for (int i = 0; i < catalogs.size(); ++i) {
            ExtensionCatalog catalog = catalogs.get(i);
            if (catalog.isPlatform() && catalog.getMetadata()
                    .get(Constants.REGISTRY_CLIENT_ALL_CATALOG_EXTENSIONS) instanceof Map<?, ?> allCatalogExtensions) {
                final ArtifactCoords managedQuarkusCore = (ArtifactCoords) allCatalogExtensions
                        .get(quarkusCore.getArtifact().getKey());
                if (managedQuarkusCore != null) {
                    if (i == 0) {
                        return catalogs;
                    }
                    catalogContainingCoreIndex = i;
                    matchingQuarkusCoreOrigin = catalog;
                    break;
                }
            }
            if (matchingQuarkusCoreOrigin == null) {
                if (allCoreOrigins == null) {
                    allCoreOrigins = getOriginCatalogsByQuarkusCore(quarkusCore.getOrigins());
                }
                matchingQuarkusCoreOrigin = allCoreOrigins.get(catalog.getQuarkusCoreVersion());
            }
        }
        if (matchingQuarkusCoreOrigin == null) {
            return List.of();
        }
        if (catalogContainingCoreIndex >= 0) {
            final List<ExtensionCatalog> result = new ArrayList<>(catalogs);
            Collections.swap(result, 0, catalogContainingCoreIndex);
            return result;
        }
        final List<ExtensionCatalog> result = new ArrayList<>(catalogs.size() + 1);
        result.add(matchingQuarkusCoreOrigin);
        result.addAll(catalogs);
        return result;
    }

    private static String noCompatibleQuarkusCoreVersion(List<Extension> extensionsToAdd) {
        final StringBuilder buf = new StringBuilder();
        buf.append("Failed to determine a compatible Quarkus version for the requested extensions: ");
        buf.append(extensionsToAdd.get(0).getArtifact().getKey().toGacString());
        for (int i = 1; i < extensionsToAdd.size(); ++i) {
            buf.append(", ").append(extensionsToAdd.get(i).getArtifact().getKey().toGacString());
        }
        return buf.toString();
    }

    private static Map<String, ExtensionCatalog> getOriginCatalogsByQuarkusCore(List<ExtensionOrigin> origins) {
        final Map<String, ExtensionCatalog> result = new HashMap<>(origins.size());
        for (ExtensionOrigin origin : origins) {
            if (origin instanceof ExtensionCatalog catalog) {
                result.putIfAbsent(catalog.getQuarkusCoreVersion(), catalog);
            }
        }
        return result;
    }

    private static boolean containsQuarkusCore(List<Extension> extensions) {
        for (Extension e : extensions) {
            if (e.getArtifact().getArtifactId().equals(QUARKUS_CORE)) {
                return true;
            }
        }
        return false;
    }

    private static Extension findQuarkusCore(ExtensionCatalog extensionCatalog) throws QuarkusCommandException {
        final Optional<Extension> quarkusCore = extensionCatalog.getExtensions().stream()
                .filter(e -> e.getArtifact().getArtifactId().equals(QUARKUS_CORE)).findFirst();
        if (quarkusCore.isEmpty()) {
            throw new QuarkusCommandException("Failed to locate quarkus-core in the extension catalog");
        }
        return quarkusCore.get();
    }

    private static void addOriginsWithPreferences(final List<ExtensionOrigins> extOrigins, Extension e) {
        ExtensionOrigins.Builder eoBuilder = null;
        for (ExtensionOrigin c : e.getOrigins()) {
            if (c instanceof ExtensionCatalog catalog) {
                final OriginPreference op = (OriginPreference) c.getMetadata().get(Constants.REGISTRY_CLIENT_ORIGIN_PREFERENCE);
                if (op != null) {
                    if (eoBuilder == null) {
                        eoBuilder = ExtensionOrigins.builder(e.getArtifact().getKey());
                    }
                    eoBuilder.addOrigin(catalog, op);
                }
            }
        }
        if (eoBuilder != null) {
            extOrigins.add(eoBuilder.build());
        }
    }

    private void checkMinimumJavaVersion(String javaVersionString, List<Extension> extensions) throws QuarkusCommandException {
        final List<Extension> incompatibleExtensions = new ArrayList<>();
        final int javaVersion = javaVersionString == null ? JavaVersion.DEFAULT_JAVA_VERSION
                : Integer.parseInt(javaVersionString);
        for (Extension extension : extensions) {
            Integer extMinJavaVersion = getMinimumJavaVersion(extension);
            if (extMinJavaVersion != null
                    && javaVersion < extMinJavaVersion) {
                incompatibleExtensions.add(extension);
            }
        }
        if (!incompatibleExtensions.isEmpty()) {
            final String list = incompatibleExtensions.stream()
                    .map(e -> String.format("- %s (min: %s)", e.managementKey(), getMinimumJavaVersion(e)))
                    .collect(Collectors.joining("\n  "));
            throw new QuarkusCommandException(String
                    .format("Some extensions are not compatible with the selected Java version (%s):\n %s", javaVersion, list));
        }
    }

    private static String getDefaultCodestart(ExtensionCatalog catalog) {
        // Recent versions of the catalog have a default-codestart in the project metadata (2.10+)
        var map = catalog.getMetadata();
        if (map != null && !map.isEmpty()) {
            var projectMetadata = map.get("project");
            if (projectMetadata instanceof Map) {
                var defaultCodestart = ((Map<?, ?>) projectMetadata).get("default-codestart");
                if (defaultCodestart != null) {
                    if (defaultCodestart instanceof String) {
                        return defaultCodestart.toString();
                    }
                }
            }
        }
        // Let's use resteasy-reactive for older versions
        return "resteasy-reactive";
    }
}

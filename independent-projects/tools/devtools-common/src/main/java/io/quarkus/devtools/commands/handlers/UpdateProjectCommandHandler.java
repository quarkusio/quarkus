package io.quarkus.devtools.commands.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.devtools.commands.UpdateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.ProjectInfoCommandHandler.PlatformInfo;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.state.ExtensionProvider;
import io.quarkus.devtools.project.state.ModuleState;
import io.quarkus.devtools.project.state.ProjectState;
import io.quarkus.devtools.project.state.TopExtensionDependency;
import io.quarkus.devtools.project.update.QuarkusUpdateCommand;
import io.quarkus.devtools.project.update.QuarkusUpdateException;
import io.quarkus.devtools.project.update.QuarkusUpdates;
import io.quarkus.devtools.project.update.QuarkusUpdatesRepository;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.selection.ExtensionOrigins;
import io.quarkus.registry.catalog.selection.OriginCombination;
import io.quarkus.registry.catalog.selection.OriginPreference;
import io.quarkus.registry.catalog.selection.OriginSelector;

public class UpdateProjectCommandHandler implements QuarkusCommandHandler {
    public static final String ADD = "Add:";
    public static final String REMOVE = "Remove:";
    public static final String UPDATE = "Update:";
    public static final String ITEM_FORMAT = "%-7s %s";

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final ApplicationModel appModel = invocation.getValue(UpdateProject.APP_MODEL);
        final ExtensionCatalog targetCatalog = invocation.getValue(UpdateProject.TARGET_CATALOG);
        final String targetPlatformVersion = invocation.getValue(UpdateProject.TARGET_PLATFORM_VERSION);

        final boolean perModule = invocation.getValue(UpdateProject.PER_MODULE, false);
        final ProjectState currentState = ProjectInfoCommandHandler.resolveProjectState(appModel,
                invocation.getQuarkusProject().getExtensionsCatalog());
        final ArtifactCoords projectQuarkusPlatformBom = getProjectQuarkusPlatformBOM(currentState);
        if (projectQuarkusPlatformBom == null) {
            invocation.log().error("The project does not import any Quarkus platform BOM");
            return QuarkusCommandOutcome.failure();
        }
        if (Objects.equals(projectQuarkusPlatformBom.getVersion(), targetPlatformVersion)) {
            ProjectInfoCommandHandler.logState(currentState, perModule, true, invocation.getQuarkusProject().log());
        } else {
            invocation.log().info("Instructions to update this project from '%s' to '%s':",
                    projectQuarkusPlatformBom.getVersion(), targetPlatformVersion);
            final QuarkusProject quarkusProject = invocation.getQuarkusProject();
            logUpdates(currentState, targetCatalog, false, perModule, quarkusProject.log());
            final boolean noRewrite = invocation.getValue(UpdateProject.NO_REWRITE, false);

            if (!noRewrite) {
                final BuildTool buildTool = quarkusProject.getExtensionManager().getBuildTool();
                String kotlinVersion = getMetadata(targetCatalog, "project", "properties", "kotlin-version");

                QuarkusUpdates.ProjectUpdateRequest request = new QuarkusUpdates.ProjectUpdateRequest(
                        buildTool,
                        projectQuarkusPlatformBom.getVersion(),
                        targetPlatformVersion,
                        kotlinVersion);
                Path recipe = null;
                try {
                    recipe = Files.createTempFile("quarkus-project-recipe-", ".yaml");
                    final String updateRecipesVersion = invocation.getValue(UpdateProject.REWRITE_UPDATE_RECIPES_VERSION,
                            QuarkusUpdatesRepository.DEFAULT_UPDATE_RECIPES_VERSION);
                    final QuarkusUpdatesRepository.FetchResult fetchResult = QuarkusUpdates.createRecipe(invocation.log(),
                            recipe,
                            QuarkusProjectHelper.artifactResolver(), buildTool, updateRecipesVersion, request);
                    final String rewritePluginVersion = invocation.getValue(UpdateProject.REWRITE_PLUGIN_VERSION,
                            fetchResult.getRewritePluginVersion());
                    final boolean rewriteDryRun = invocation.getValue(UpdateProject.REWRITE_DRY_RUN, false);
                    invocation.log().warn(
                            "The update feature does not yet handle updates of the extension versions. If needed, update your extensions manually.");
                    QuarkusUpdateCommand.handle(
                            invocation.log(),
                            buildTool,
                            quarkusProject.getProjectDirPath(),
                            rewritePluginVersion,
                            fetchResult.getRecipesGAV(),
                            recipe,
                            rewriteDryRun);
                } catch (IOException e) {
                    throw new QuarkusCommandException("Error while generating the project update script", e);
                } catch (QuarkusUpdateException e) {
                    throw new QuarkusCommandException("Error while running the project update script", e);
                } finally {
                    if (recipe != null) {
                        try {
                            Files.deleteIfExists(recipe);
                        } catch (IOException e) {
                            // ignore
                        }
                    }

                }
            }
        }
        return QuarkusCommandOutcome.success();
    }

    private static ArtifactCoords getProjectQuarkusPlatformBOM(ProjectState currentState) {
        for (ArtifactCoords c : currentState.getPlatformBoms()) {
            if (c.getArtifactId().equals(ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID)) {
                return c;
            }
        }
        return null;
    }

    private static void logUpdates(ProjectState currentState, ExtensionCatalog recommendedCatalog, boolean recommendState,
            boolean perModule, MessageWriter log) {
        if (currentState.getPlatformBoms().isEmpty()) {
            log.info("The project does not import any Quarkus platform BOM");
            return;
        }
        if (currentState.getExtensions().isEmpty()) {
            log.info("Quarkus extension were not found among the project dependencies");
            return;
        }
        final ProjectState recommendedState = resolveRecommendedState(currentState, recommendedCatalog, log);
        if (currentState == recommendedState) {
            log.info("The project is up-to-date");
            return;
        }

        if (recommendState) {
            ProjectInfoCommandHandler.logState(recommendedState, perModule, false, log);
            return;
        }

        // log instructions
        final Map<ArtifactKey, PlatformInfo> platformImports = new LinkedHashMap<>();
        for (ArtifactCoords c : currentState.getPlatformBoms()) {
            final PlatformInfo info = new PlatformInfo();
            info.imported = c;
            platformImports.put(c.getKey(), info);
        }
        List<PlatformInfo> importVersionUpdates = new ArrayList<>();
        List<PlatformInfo> newImports = new ArrayList<>(0);
        for (ArtifactCoords c : recommendedState.getPlatformBoms()) {
            final PlatformInfo importInfo = platformImports.computeIfAbsent(c.getKey(), k -> new PlatformInfo());
            importInfo.recommended = c;
            if (importInfo.isToBeImported()) {
                newImports.add(importInfo);
            } else if (importInfo.isVersionUpdateRecommended()) {
                importVersionUpdates.add(importInfo);
            }
        }

        log.info("");
        final boolean importsToBeRemoved = platformImports.values().stream().filter(p -> p.recommended == null).findFirst()
                .isPresent();
        final boolean platformUpdatesAvailable = !importVersionUpdates.isEmpty() || !newImports.isEmpty() || importsToBeRemoved;
        if (platformUpdatesAvailable) {
            log.info("Recommended Quarkus platform BOM updates:");
            if (!importVersionUpdates.isEmpty()) {
                for (PlatformInfo importInfo : importVersionUpdates) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            UpdateProjectCommandHandler.UPDATE, importInfo.imported.toCompactCoords()) + " -> "
                            + importInfo.getRecommendedVersion());
                }
            }
            if (!newImports.isEmpty()) {
                for (PlatformInfo importInfo : newImports) {
                    log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            UpdateProjectCommandHandler.ADD, importInfo.recommended.toCompactCoords()));
                }
            }
            if (importsToBeRemoved) {
                for (PlatformInfo importInfo : platformImports.values()) {
                    if (importInfo.recommended == null) {
                        log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                UpdateProjectCommandHandler.REMOVE, importInfo.imported.toCompactCoords()));
                    }
                }
            }
            log.info("");
        }

        final ExtensionMap extensionInfo = new ExtensionMap(currentState.getExtensions().size());
        for (TopExtensionDependency dep : currentState.getExtensions()) {
            extensionInfo.add(new ExtensionInfo(dep));
        }
        for (TopExtensionDependency dep : recommendedState.getExtensions()) {
            final ExtensionInfo info = extensionInfo.get(dep.getKey());
            if (info != null) {
                info.recommendedDep = dep;
            }
        }
        final Map<String, List<ExtensionInfo>> versionedManagedExtensions = new LinkedHashMap<>(0);
        final Map<String, List<ArtifactCoords>> removedExtensions = new LinkedHashMap<>(0);
        final Map<String, List<ArtifactCoords>> addedExtensions = new LinkedHashMap<>(0);
        final Map<String, List<ExtensionInfo>> nonPlatformExtensionUpdates = new LinkedHashMap<>();
        for (ExtensionInfo info : extensionInfo.values()) {
            if (!info.isUpdateRecommended()) {
                continue;
            }
            if (!info.currentDep.getKey().equals(info.getRecommendedDependency().getKey())) {
                if (info.currentDep.isPlatformExtension()) {
                    removedExtensions.computeIfAbsent(info.currentDep.getProviderKey(), k -> new ArrayList<>())
                            .add(info.currentDep.getArtifact());
                } else {
                    nonPlatformExtensionUpdates.computeIfAbsent(info.currentDep.getProviderKey(), k -> new ArrayList<>())
                            .add(info);
                }
                if (info.getRecommendedDependency().isPlatformExtension()) {
                    addedExtensions.computeIfAbsent(info.getRecommendedDependency().getProviderKey(), k -> new ArrayList<>())
                            .add(info.getRecommendedDependency().getArtifact());
                } else {
                    nonPlatformExtensionUpdates
                            .computeIfAbsent(info.getRecommendedDependency().getProviderKey(), k -> new ArrayList<>())
                            .add(info);
                }
            } else if (info.getRecommendedDependency().isPlatformExtension()) {
                if (info.currentDep.isNonRecommendedVersion()) {
                    versionedManagedExtensions
                            .computeIfAbsent(info.getRecommendedDependency().getProviderKey(), k -> new ArrayList<>())
                            .add(info);
                }
            } else if (!info.currentDep.getVersion().equals(info.getRecommendedDependency().getVersion())) {
                nonPlatformExtensionUpdates
                        .computeIfAbsent(info.getRecommendedDependency().getProviderKey(), k -> new ArrayList<>()).add(info);
            }
        }

        if (versionedManagedExtensions.isEmpty()
                && removedExtensions.isEmpty()
                && addedExtensions.isEmpty()
                && nonPlatformExtensionUpdates.isEmpty()) {
            if (!platformUpdatesAvailable) {
                log.info("The project is up-to-date");
            }
            return;
        }

        for (PlatformInfo platform : platformImports.values()) {
            final String provider = platform.getRecommendedProviderKey();
            if (!versionedManagedExtensions.containsKey(provider)
                    && !removedExtensions.containsKey(provider)
                    && !addedExtensions.containsKey(provider)) {
                continue;
            }
            log.info("Extensions from " + platform.getRecommendedProviderKey() + ":");
            for (ExtensionInfo e : versionedManagedExtensions.getOrDefault(provider, Collections.emptyList())) {
                final StringBuilder sb = new StringBuilder();
                sb.append(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                        UpdateProjectCommandHandler.UPDATE, e.currentDep.getArtifact().toCompactCoords()));
                sb.append(" -> remove version (managed)");
                log.info(sb.toString());
            }
            for (ArtifactCoords e : addedExtensions.getOrDefault(provider, Collections.emptyList())) {
                log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, UpdateProjectCommandHandler.ADD,
                        e.getKey().toGacString()));
            }
            for (ArtifactCoords e : removedExtensions.getOrDefault(provider, Collections.emptyList())) {
                log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, UpdateProjectCommandHandler.REMOVE,
                        e.getKey().toGacString()));
            }
            log.info("");
        }

        if (!nonPlatformExtensionUpdates.isEmpty()) {
            for (Map.Entry<String, List<ExtensionInfo>> provider : nonPlatformExtensionUpdates.entrySet()) {
                log.info("Extensions from " + provider.getKey() + ":");
                for (ExtensionInfo info : provider.getValue()) {
                    if (info.currentDep.isPlatformExtension()) {
                        log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                UpdateProjectCommandHandler.ADD,
                                info.getRecommendedDependency().getArtifact().toCompactCoords()));
                    } else if (info.getRecommendedDependency().isPlatformExtension()) {
                        log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                UpdateProjectCommandHandler.REMOVE, info.currentDep.getArtifact().toCompactCoords()));
                    } else {
                        log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                UpdateProjectCommandHandler.UPDATE, info.currentDep.getArtifact().toCompactCoords() + " -> "
                                        + info.getRecommendedDependency().getVersion()));
                    }
                }
                log.info("");
            }
        }
    }

    private static ProjectState resolveRecommendedState(ProjectState currentState, ExtensionCatalog latestCatalog,
            MessageWriter log) {
        if (currentState.getPlatformBoms().isEmpty()) {
            return currentState;
        }
        if (currentState.getExtensions().isEmpty()) {
            return currentState;
        }

        final ExtensionMap extensionInfo = new ExtensionMap();
        for (TopExtensionDependency dep : currentState.getExtensions()) {
            extensionInfo.add(new ExtensionInfo(dep));
        }

        for (Extension e : latestCatalog.getExtensions()) {
            final ExtensionInfo candidate = extensionInfo.get(e.getArtifact().getKey());
            if (candidate != null && candidate.latestMetadata == null) {
                // if the latestMetadata has already been initialized, it's already the preferred one
                // that could happen if an artifact has relocated
                candidate.latestMetadata = e;
            }
        }

        final List<ExtensionInfo> unknownExtensions = new ArrayList<>(0);
        final List<Extension> updateCandidates = new ArrayList<>(extensionInfo.size());
        final Map<String, ExtensionMap> updateCandidatesByOrigin = new HashMap<>();
        for (ExtensionInfo i : extensionInfo.values()) {
            if (i.latestMetadata == null) {
                unknownExtensions.add(i);
            } else {
                updateCandidates.add(i.latestMetadata);
                for (ExtensionOrigin o : i.latestMetadata.getOrigins()) {
                    updateCandidatesByOrigin.computeIfAbsent(o.getId(), k -> new ExtensionMap()).add(i);
                }
            }
        }

        if (extensionInfo.isEmpty()) {
            return currentState;
        }

        if (!unknownExtensions.isEmpty()) {
            log.warn(
                    "The configured Quarkus registries did not provide any compatibility information for the following extensions in the context of the currently recommended Quarkus platforms:");
            unknownExtensions.forEach(e -> log.warn(" " + e.currentDep.getArtifact().toCompactCoords()));
        }

        final List<ExtensionCatalog> recommendedOrigins;
        try {
            recommendedOrigins = getRecommendedOrigins(latestCatalog, updateCandidates);
        } catch (QuarkusCommandException e) {
            log.info("Failed to find a compatible configuration update for the project");
            return currentState;
        }

        int collectedUpdates = 0;
        for (ExtensionCatalog recommendedOrigin : recommendedOrigins) {
            final ExtensionMap candidates = updateCandidatesByOrigin.get(recommendedOrigin.getId());
            for (Extension e : recommendedOrigin.getExtensions()) {
                final ExtensionInfo info = candidates.get(e.getArtifact().getKey());
                if (info != null && info.recommendedMetadata == null) {
                    info.setRecommendedMetadata(e);
                    if (++collectedUpdates == updateCandidates.size()) {
                        break;
                    }
                }
            }
        }

        final ProjectState.Builder stateBuilder = ProjectState.builder();
        for (ExtensionCatalog c : recommendedOrigins) {
            if (c.isPlatform()) {
                stateBuilder.addPlatformBom(c.getBom());
            }
        }

        final Map<String, ExtensionProvider.Builder> extProviders = new LinkedHashMap<>(recommendedOrigins.size());
        for (ExtensionInfo info : extensionInfo.values()) {
            final TopExtensionDependency ext = info.getRecommendedDependency();
            stateBuilder.addExtensionDependency(ext);
            extProviders.computeIfAbsent(ext.getProviderKey(), k -> ExtensionProvider.builder().setOrigin(ext.getOrigin()))
                    .addExtension(ext);
        }

        extProviders.values().forEach(b -> stateBuilder.addExtensionProvider(b.build()));

        for (ModuleState module : currentState.getModules()) {
            final ModuleState.Builder moduleBuilder = ModuleState.builder()
                    .setMainModule(module.isMain())
                    .setWorkspaceModule(module.getWorkspaceModule());
            for (TopExtensionDependency dep : module.getExtensions()) {
                final TopExtensionDependency recommendedDep = extensionInfo.get(dep.getKey()).getRecommendedDependency();
                moduleBuilder.addExtensionDependency(recommendedDep);
                final ExtensionOrigin origin = recommendedDep.getOrigin();
                if (origin != null && origin.isPlatform()) {
                    moduleBuilder.addPlatformBom(origin.getBom());
                }
            }
            stateBuilder.addModule(moduleBuilder.build());
        }

        return stateBuilder.build();
    }

    private static class ExtensionInfo {
        final TopExtensionDependency currentDep;
        Extension latestMetadata;
        Extension recommendedMetadata;
        TopExtensionDependency recommendedDep;

        ExtensionInfo(TopExtensionDependency currentDep) {
            this.currentDep = currentDep;
        }

        void setRecommendedMetadata(Extension e) {
            this.recommendedMetadata = e;
            if (!currentDep.getArtifact().getKey().equals(e.getArtifact().getKey())) {
            }
        }

        Extension getRecommendedMetadata() {
            if (recommendedMetadata != null) {
                return recommendedMetadata;
            }
            if (recommendedDep == null) {
                return currentDep.getCatalogMetadata();
            }
            return recommendedMetadata = currentDep.getCatalogMetadata();
        }

        TopExtensionDependency getRecommendedDependency() {
            if (recommendedDep != null) {
                return recommendedDep;
            }
            if (recommendedMetadata == null) {
                return currentDep;
            }
            return recommendedDep = TopExtensionDependency.builder()
                    .setArtifact(recommendedMetadata.getArtifact())
                    .setCatalogMetadata(recommendedMetadata)
                    .setTransitive(currentDep.isTransitive())
                    .build();
        }

        boolean isUpdateRecommended() {
            return getRecommendedDependency() != currentDep;
        }
    }

    private static List<ExtensionCatalog> getRecommendedOrigins(ExtensionCatalog extensionCatalog, List<Extension> extensions)
            throws QuarkusCommandException {
        final List<ExtensionOrigins> extOrigins = new ArrayList<>(extensions.size());
        for (Extension e : extensions) {
            addOrigins(extOrigins, e);
        }

        final OriginCombination recommendedCombination = OriginSelector.of(extOrigins).calculateRecommendedCombination();
        if (recommendedCombination == null) {
            final StringBuilder buf = new StringBuilder();
            buf.append("Failed to determine a compatible Quarkus version for the requested extensions: ");
            buf.append(extensions.get(0).getArtifact().getKey().toGacString());
            for (int i = 1; i < extensions.size(); ++i) {
                buf.append(", ").append(extensions.get(i).getArtifact().getKey().toGacString());
            }
            throw new QuarkusCommandException(buf.toString());
        }
        return recommendedCombination.getUniqueSortedOrigins().stream().map(o -> o.getCatalog()).collect(Collectors.toList());
    }

    private static void addOrigins(final List<ExtensionOrigins> extOrigins, Extension e) {
        ExtensionOrigins.Builder eoBuilder = null;
        for (ExtensionOrigin o : e.getOrigins()) {
            if (!(o instanceof ExtensionCatalog)) {
                continue;
            }
            final ExtensionCatalog c = (ExtensionCatalog) o;
            final OriginPreference op = (OriginPreference) c.getMetadata().get("origin-preference");
            if (op == null) {
                continue;
            }
            if (eoBuilder == null) {
                eoBuilder = ExtensionOrigins.builder(e.getArtifact().getKey());
            }
            eoBuilder.addOrigin(c, op);
        }
        if (eoBuilder != null) {
            extOrigins.add(eoBuilder.build());
        }
    }

    private static class ExtensionMap {
        final Map<String, List<ExtensionInfo>> extensionInfo;
        final List<ExtensionInfo> list = new ArrayList<>();

        ExtensionMap() {
            this.extensionInfo = new LinkedHashMap<>();
        }

        ExtensionMap(int size) {
            this.extensionInfo = new LinkedHashMap<>(size);
        }

        void add(ExtensionInfo e) {
            extensionInfo.put(e.currentDep.getArtifact().getArtifactId(), Collections.singletonList(e));
            list.add(e);
        }

        ExtensionInfo get(ArtifactKey key) {
            final List<ExtensionInfo> list = extensionInfo.get(key.getArtifactId());
            if (list == null || list.isEmpty()) {
                return null;
            }
            if (list.size() == 1) {
                return list.get(0);
            }
            for (ExtensionInfo e : list) {
                if (e.currentDep.getKey().equals(key)
                        || e.getRecommendedDependency() != null && e.getRecommendedDependency().getKey().equals(key)) {
                    return e;
                }
            }
            throw new IllegalArgumentException(key + " isn't found in the extension map");
        }

        Collection<ExtensionInfo> values() {
            return list;
        }

        int size() {
            return extensionInfo.size();
        }

        boolean isEmpty() {
            return extensionInfo.isEmpty();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> T getMetadata(ExtensionCatalog catalog, String... path) {
        Object currentValue = catalog.getMetadata();
        for (String pathElement : path) {
            if (!(currentValue instanceof Map)) {
                return null;
            }

            currentValue = ((Map) currentValue).get(pathElement);
        }

        return (T) currentValue;
    }
}

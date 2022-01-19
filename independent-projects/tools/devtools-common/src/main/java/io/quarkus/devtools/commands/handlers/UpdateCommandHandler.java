package io.quarkus.devtools.commands.handlers;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.InfoCommandHandler.PlatformInfo;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.state.ExtensionProvider;
import io.quarkus.devtools.project.state.ModuleState;
import io.quarkus.devtools.project.state.ProjectState;
import io.quarkus.devtools.project.state.TopExtensionDependency;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.selection.ExtensionOrigins;
import io.quarkus.registry.catalog.selection.OriginCombination;
import io.quarkus.registry.catalog.selection.OriginPreference;
import io.quarkus.registry.catalog.selection.OriginSelector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateCommandHandler implements QuarkusCommandHandler {

    public static final String APP_MODEL = "app-model";
    public static final String LATEST_CATALOG = "latest-catalog";
    public static final String LOG_RECOMMENDED_STATE = "log-recommended-state";
    public static final String LOG_STATE_PER_MODULE = "log-state-per-module";
    public static final String RECTIFY = "rectify";

    public static final String ADD = "Add:";
    public static final String REMOVE = "Remove:";
    public static final String UPDATE = "Update:";

    public static final String PLATFORM_RECTIFY_FORMAT = "%-7s %s";

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {

        final ApplicationModel appModel = invocation.getValue(APP_MODEL);
        final ExtensionCatalog latestCatalog = invocation.getValue(UpdateCommandHandler.LATEST_CATALOG);
        final boolean logRecommendedState = invocation.getValue(UpdateCommandHandler.LOG_RECOMMENDED_STATE, false);
        final boolean logStatePerModule = invocation.getValue(UpdateCommandHandler.LOG_STATE_PER_MODULE, false);
        final boolean rectify = invocation.getValue(UpdateCommandHandler.RECTIFY, false);

        if (logStatePerModule && !rectify) {
            throw new QuarkusCommandException("Update per module isn't supported yet!");
        }

        final ProjectState currentState = InfoCommandHandler.resolveProjectState(appModel,
                invocation.getQuarkusProject().getExtensionsCatalog());

        if (rectify) {
            InfoCommandHandler.logState(currentState, logStatePerModule, rectify, invocation.getQuarkusProject().log());
        } else {
            logUpdates(currentState, latestCatalog, logRecommendedState, logStatePerModule,
                    invocation.getQuarkusProject().log());
        }

        return QuarkusCommandOutcome.success();
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
            log.info("No recommended updates found");
            return;
        }

        if (recommendState) {
            InfoCommandHandler.logState(recommendedState, perModule, false, log);
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
            if (importInfo.isImported()) {
                importVersionUpdates.add(importInfo);
            } else {
                newImports.add(importInfo);
            }
        }

        log.info("");
        final boolean importsToBeRemoved = importVersionUpdates.size() + newImports.size() < platformImports.size();
        if (!importVersionUpdates.isEmpty() || !newImports.isEmpty() || importsToBeRemoved) {
            log.info("Recommended Quarkus platform BOM updates:");
            if (!importVersionUpdates.isEmpty()) {
                for (PlatformInfo importInfo : importVersionUpdates) {
                    log.info(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT,
                            UpdateCommandHandler.UPDATE, importInfo.imported.toCompactCoords()) + " -> "
                            + importInfo.getRecommendedVersion());
                }
            }
            if (!newImports.isEmpty()) {
                for (PlatformInfo importInfo : newImports) {
                    log.info(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT,
                            UpdateCommandHandler.ADD, importInfo.recommended.toCompactCoords()));
                }
            }
            if (importsToBeRemoved) {
                for (PlatformInfo importInfo : platformImports.values()) {
                    if (importInfo.recommended == null) {
                        log.info(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT,
                                UpdateCommandHandler.REMOVE, importInfo.imported.toCompactCoords()));
                    }
                }
            }
            log.info("");
        }

        final Map<ArtifactKey, ExtensionInfo> extensionInfo = new LinkedHashMap<>(currentState.getExtensions().size());
        for (TopExtensionDependency dep : currentState.getExtensions()) {
            extensionInfo.put(dep.getKey(), new ExtensionInfo(dep));
        }
        for (TopExtensionDependency dep : recommendedState.getExtensions()) {
            final ExtensionInfo info = extensionInfo.get(dep.getKey());
            if (info != null) {
                info.recommendedDep = dep;
            }
        }
        final Map<ArtifactCoords, List<ArtifactCoords>> versionedManagedExtensions = new LinkedHashMap<>(0);
        final Map<String, List<ExtensionInfo>> nonPlatformExtensionUpdates = new LinkedHashMap<>();
        for (ExtensionInfo info : extensionInfo.values()) {
            if (info.getRecommendedDependency() == null) {
                continue;
            }
            if (info.getRecommendedDependency().isPlatformExtension()) {
                if (info.currentDep.isNonRecommendedVersion()) {
                    versionedManagedExtensions
                            .computeIfAbsent(info.getRecommendedDependency().getOrigin().getBom(), k -> new ArrayList<>())
                            .add(info.currentDep.getArtifact());
                }
            } else if (!info.currentDep.getVersion().equals(info.getRecommendedDependency().getVersion())) {
                nonPlatformExtensionUpdates.computeIfAbsent(info.currentDep.getProviderKey(), k -> new ArrayList<>()).add(info);
            }
        }

        if (!versionedManagedExtensions.isEmpty()) {
            for (Map.Entry<ArtifactCoords, List<ArtifactCoords>> origin : versionedManagedExtensions.entrySet()) {
                log.info("Extensions from " + origin.getKey().toCompactCoords() + ":");
                for (ArtifactCoords c : origin.getValue()) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT,
                            UpdateCommandHandler.UPDATE, c.toCompactCoords()));
                    sb.append(" -> remove version (managed)");
                    log.info(sb.toString());
                }
                log.info("");
            }
        }

        if (!nonPlatformExtensionUpdates.isEmpty()) {
            for (Map.Entry<String, List<ExtensionInfo>> provider : nonPlatformExtensionUpdates.entrySet()) {
                log.info("Extensions from " + provider.getKey() + ":");
                for (ExtensionInfo info : provider.getValue()) {
                    log.info(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT,
                            UpdateCommandHandler.UPDATE, info.currentDep.getArtifact().toCompactCoords() + " -> "
                                    + info.getRecommendedDependency().getVersion()));
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

        final Map<ArtifactKey, ExtensionInfo> extensionInfo = new HashMap<>();
        for (TopExtensionDependency dep : currentState.getExtensions()) {
            extensionInfo.put(dep.getKey(), new ExtensionInfo(dep));
        }

        for (Extension e : latestCatalog.getExtensions()) {
            final ExtensionInfo candidate = extensionInfo.get(e.getArtifact().getKey());
            if (candidate != null) {
                candidate.latestMetadata = e;
            }
        }

        final List<ArtifactCoords> unknownExtensions = new ArrayList<>(0);
        final List<Extension> updateCandidates = new ArrayList<>(extensionInfo.size());
        final Map<String, Map<ArtifactKey, ExtensionInfo>> updateCandidatesByOrigin = new HashMap<>();
        for (ExtensionInfo i : extensionInfo.values()) {
            if (i.latestMetadata == null) {
                unknownExtensions.add(i.currentDep.getArtifact());
            } else {
                updateCandidates.add(i.latestMetadata);
                for (ExtensionOrigin o : i.latestMetadata.getOrigins()) {
                    updateCandidatesByOrigin.computeIfAbsent(o.getId(), k -> new HashMap<>()).put(i.currentDep.getKey(), i);
                }
            }
        }

        if (extensionInfo.isEmpty()) {
            return currentState;
        }

        if (!unknownExtensions.isEmpty()) {
            log.warn(
                    "The following extensions may be incompatible with the recommended updates because the configured Quarkus registries did not provide any compatibility information for them in the context of the currently recommended Quarkus platforms:");
            unknownExtensions.forEach(e -> log.warn(" " + e.getKey().toGacString()));
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
            final Map<ArtifactKey, ExtensionInfo> candidates = updateCandidatesByOrigin.get(recommendedOrigin.getId());
            for (Extension e : recommendedOrigin.getExtensions()) {
                final ExtensionInfo info = candidates.get(e.getArtifact().getKey());
                if (info != null && info.recommendedMetadata == null) {
                    info.recommendedMetadata = e;
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
}

package io.quarkus.devtools.project.update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.state.ExtensionProvider;
import io.quarkus.devtools.project.state.ModuleState;
import io.quarkus.devtools.project.state.ProjectState;
import io.quarkus.devtools.project.state.TopExtensionDependency;
import io.quarkus.devtools.project.update.ExtensionMapBuilder.ExtensionUpdateInfoBuilder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.selection.ExtensionOrigins;
import io.quarkus.registry.catalog.selection.OriginCombination;
import io.quarkus.registry.catalog.selection.OriginPreference;
import io.quarkus.registry.catalog.selection.OriginSelector;

public final class ProjectUpdateInfos {

    private ProjectUpdateInfos() {
    }

    public static ProjectExtensionsUpdateInfo resolveExtensionsUpdateInfo(ProjectState currentState,
            ProjectState recommendedState) {
        checkProjectState(currentState, recommendedState);

        final ExtensionMapBuilder extensionInfo = new ExtensionMapBuilder(currentState.getExtensions().size());
        for (TopExtensionDependency dep : currentState.getExtensions()) {
            extensionInfo.add(new ExtensionUpdateInfoBuilder(dep));
        }
        for (TopExtensionDependency dep : recommendedState.getExtensions()) {
            final ExtensionUpdateInfoBuilder info = extensionInfo.get(dep.getKey());
            if (info != null) {
                info.setRecommendedDep(dep);
            }
        }
        final Map<String, List<ExtensionUpdateInfo>> extensions = new LinkedHashMap<>(0);
        for (ExtensionUpdateInfoBuilder infoBuilder : extensionInfo.values()) {
            final ExtensionUpdateInfo info = infoBuilder.build();
            if (!info.isUpdateRecommended()) {
                continue;
            }
            extensions.computeIfAbsent(info.getRecommendedDependency().getProviderKey(), k -> new ArrayList<>())
                    .add(info);
        }
        return new ProjectExtensionsUpdateInfo(extensions);
    }

    public static ProjectPlatformUpdateInfo resolvePlatformUpdateInfo(ProjectState currentState,
            ProjectState recommendedState) {
        checkProjectState(currentState, recommendedState);

        final Map<ArtifactKey, PlatformInfo> platformImports = new LinkedHashMap<>();
        for (ArtifactCoords c : currentState.getPlatformBoms()) {
            final PlatformInfo info = new PlatformInfo(c, null);
            platformImports.put(c.getKey(), info);
        }
        List<PlatformInfo> importVersionUpdates = new ArrayList<>();
        List<PlatformInfo> newImports = new ArrayList<>(0);
        for (ArtifactCoords c : recommendedState.getPlatformBoms()) {
            final PlatformInfo importInfo = platformImports.compute(c.getKey(), (k, v) -> {
                if (v == null) {
                    return new PlatformInfo(null, c);
                }
                return new PlatformInfo(v.getImported(), c);
            });
            if (importInfo.isToBeImported()) {
                newImports.add(importInfo);
            } else if (importInfo.isVersionUpdateRecommended()) {
                importVersionUpdates.add(importInfo);
            }
        }
        return new ProjectPlatformUpdateInfo(platformImports, importVersionUpdates, newImports);
    }

    private static void checkProjectState(ProjectState currentState, ProjectState recommendedState) {
        if (currentState.getPlatformBoms().isEmpty()) {
            throw new IllegalStateException("The project does not import any Quarkus platform BOM");
        }
    }

    public static ProjectState resolveRecommendedState(ProjectState currentState, ExtensionCatalog recommendedCatalog,
            MessageWriter log) {
        if (currentState.getPlatformBoms().isEmpty()) {
            return currentState;
        }
        if (currentState.getExtensions().isEmpty()) {
            return currentState;
        }

        final ExtensionMapBuilder builder = new ExtensionMapBuilder();
        for (TopExtensionDependency dep : currentState.getExtensions()) {
            builder.add(new ExtensionUpdateInfoBuilder(dep));
        }

        for (Extension e : recommendedCatalog.getExtensions()) {
            final ExtensionUpdateInfoBuilder candidate = builder.get(e.getArtifact().getKey());
            if (candidate != null && candidate.getLatestMetadata() == null) {
                // if the latestMetadata has already been initialized, it's already the preferred one
                // that could happen if an artifact has relocated
                candidate.setLatestMetadata(e);
            }
        }

        final List<ExtensionUpdateInfoBuilder> unknownExtensions = new ArrayList<>(0);
        final List<Extension> updateCandidates = new ArrayList<>(builder.size());
        final Map<String, ExtensionMapBuilder> updateCandidatesByOrigin = new HashMap<>();
        for (ExtensionUpdateInfoBuilder i : builder.values()) {
            if (i.getLatestMetadata() == null) {
                unknownExtensions.add(i);
            } else {
                updateCandidates.add(i.getLatestMetadata());
                for (ExtensionOrigin o : i.getLatestMetadata().getOrigins()) {
                    updateCandidatesByOrigin.computeIfAbsent(o.getId(), k -> new ExtensionMapBuilder()).add(i);
                }
            }
        }

        if (builder.isEmpty()) {
            return currentState;
        }

        if (!unknownExtensions.isEmpty()) {
            log.warn(
                    "The configured Quarkus registries did not provide any compatibility information for the following extensions in the context of the currently recommended Quarkus platforms:");
            unknownExtensions.forEach(e -> log.warn(" " + e.getCurrentDep().getArtifact().toCompactCoords()));
        }

        final List<ExtensionCatalog> recommendedOrigins;
        try {
            recommendedOrigins = getRecommendedOrigins(updateCandidates);
        } catch (QuarkusCommandException e) {
            log.warn("Failed to find a compatible configuration update for the project");
            return currentState;
        }

        int collectedUpdates = 0;
        for (ExtensionCatalog recommendedOrigin : recommendedOrigins) {
            final ExtensionMapBuilder candidates = updateCandidatesByOrigin.get(recommendedOrigin.getId());
            for (Extension e : recommendedOrigin.getExtensions()) {
                final ExtensionUpdateInfoBuilder info = candidates.get(e.getArtifact().getKey());
                if (info != null && info.getRecommendedMetadata() == null) {
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
        for (ExtensionUpdateInfoBuilder info : builder.values()) {
            final TopExtensionDependency ext = info.resolveRecommendedDep();
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
                final TopExtensionDependency recommendedDep = builder.get(dep.getKey()).resolveRecommendedDep();
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

    private static List<ExtensionCatalog> getRecommendedOrigins(List<Extension> extensions)
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
        return recommendedCombination.getUniqueSortedCatalogs();
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

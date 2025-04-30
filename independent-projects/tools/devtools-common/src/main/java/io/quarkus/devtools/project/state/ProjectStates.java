package io.quarkus.devtools.project.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.registry.catalog.ExtensionCatalog;

public final class ProjectStates {

    private ProjectStates() {
    }

    public static ProjectState resolveProjectState(ApplicationModel appModel, ExtensionCatalog currentCatalog) {
        final ProjectState.Builder projectBuilder = ProjectState.builder();

        final Collection<ArtifactCoords> importedPlatformBoms = appModel.getPlatforms().getImportedPlatformBoms();
        if (importedPlatformBoms.isEmpty()) {
            return projectBuilder.build();
        }

        final Map<String, ExtensionProvider.Builder> extProviderBuilders = new LinkedHashMap<>(importedPlatformBoms.size());
        importedPlatformBoms.forEach(bom -> {
            projectBuilder.addPlatformBom(bom);
            extProviderBuilders.put(ExtensionProvider.key(bom, true),
                    ExtensionProvider.builder().setArtifact(bom).setPlatform(true));
        });

        final Map<WorkspaceModuleId, ModuleState.Builder> projectModuleBuilders = new HashMap<>();
        final Map<ArtifactKey, List<ModuleState.Builder>> directModuleDeps = new HashMap<>();

        final WorkspaceModule appModule = appModel.getAppArtifact().getWorkspaceModule();
        if (appModule != null) {
            final ModuleState.Builder module = ModuleState.builder().setWorkspaceModule(appModule).setMainModule(true);
            projectModuleBuilders.put(appModule.getId(), module);
            appModule.getDirectDependencies()
                    .forEach(d -> directModuleDeps.computeIfAbsent(d.getKey(), dk -> new ArrayList<>()).add(module));

            for (Dependency constraint : appModule.getDirectDependencyConstraints()) {
                if (extProviderBuilders.containsKey(constraint.toCompactCoords())) {
                    module.addPlatformBom(constraint);
                }
            }

        }
        for (ResolvedDependency dep : appModel.getDependencies()) {
            if (dep.getWorkspaceModule() != null) {
                projectModuleBuilders.computeIfAbsent(dep.getWorkspaceModule().getId(), k -> {
                    final ModuleState.Builder module = ModuleState.builder()
                            .setWorkspaceModule(dep.getWorkspaceModule());
                    dep.getWorkspaceModule().getDirectDependencies().forEach(
                            d -> directModuleDeps.computeIfAbsent(d.getKey(), dk -> new ArrayList<>()).add(module));
                    return module;
                });
            }
        }

        final Map<ArtifactKey, TopExtensionDependency.Builder> directExtDeps = new HashMap<>();
        for (ResolvedDependency dep : appModel.getDependencies()) {
            if (dep.isFlagSet(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT)) {
                directExtDeps.put(dep.getKey(), TopExtensionDependency.builder().setResolvedDependency(dep)
                        .setTransitive(!directModuleDeps.containsKey(dep.getKey())));
            } else if (dep.isRuntimeExtensionArtifact() && directModuleDeps.containsKey(dep.getKey())) {
                directExtDeps.put(dep.getKey(), TopExtensionDependency.builder().setResolvedDependency(dep));
            }
        }

        if (directExtDeps.isEmpty()) {
            return projectBuilder.build();
        }

        currentCatalog.getExtensions().forEach(e -> {
            final ArtifactKey key = e.getArtifact().getKey();
            final TopExtensionDependency.Builder dep = directExtDeps.get(key);
            if (dep != null) {
                dep.setCatalogMetadata(e);
            }
        });

        for (TopExtensionDependency.Builder extBuilder : directExtDeps.values()) {
            final List<ModuleState.Builder> modules = directModuleDeps.getOrDefault(extBuilder.getKey(),
                    Collections.emptyList());
            final TopExtensionDependency dep = extBuilder.setTransitive(modules.isEmpty()).build();
            projectBuilder.addExtensionDependency(dep);
            for (ModuleState.Builder module : modules) {
                module.addExtensionDependency(dep);
            }
            final ExtensionProvider.Builder provider = extProviderBuilders.computeIfAbsent(dep.getProviderKey(),
                    k -> ExtensionProvider.builder().setOrigin(dep.getOrigin()));
            provider.addExtension(dep);
        }

        for (ExtensionProvider.Builder builder : extProviderBuilders.values()) {
            projectBuilder.addExtensionProvider(builder.build());
        }

        for (ModuleState.Builder builder : projectModuleBuilders.values()) {
            projectBuilder.addModule(builder.build());
        }

        return projectBuilder.build();
    }

}

package io.quarkus.devtools.commands.handlers;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.state.ExtensionProvider;
import io.quarkus.devtools.project.state.ModuleState;
import io.quarkus.devtools.project.state.ProjectState;
import io.quarkus.devtools.project.state.TopExtensionDependency;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InfoCommandHandler implements QuarkusCommandHandler {

    public static final String APP_MODEL = "app-model";
    public static final String LOG_STATE_PER_MODULE = "log-state-per-module";
    public static final String RECOMMENDATIONS_AVAILABLE = "recommendations-available";

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {

        final ApplicationModel appModel = invocation.getValue(APP_MODEL);
        final boolean logStatePerModule = invocation.getValue(UpdateCommandHandler.LOG_STATE_PER_MODULE, false);

        final boolean recommendationsAvailable = logState(
                resolveProjectState(appModel, invocation.getExtensionsCatalog()), logStatePerModule, false,
                invocation.log());
        return QuarkusCommandOutcome.success().setValue(RECOMMENDATIONS_AVAILABLE, recommendationsAvailable);
    }

    // TODO: instead of returning a boolean, the info about available
    // recommendations should be reflected in ProjectState
    protected static boolean logState(ProjectState projectState, boolean perModule, boolean rectify,
            MessageWriter log) {

        boolean recommendationsAvailable = false;

        final Map<ArtifactKey, PlatformInfo> providerInfo = new LinkedHashMap<>();
        for (ArtifactCoords bom : projectState.getPlatformBoms()) {
            providerInfo.computeIfAbsent(bom.getKey(), k -> new PlatformInfo()).imported = bom;
        }
        for (TopExtensionDependency dep : projectState.getExtensions()) {
            final ExtensionOrigin origin = dep.getOrigin();
            if (origin != null && origin.isPlatform()) {
                providerInfo.computeIfAbsent(origin.getBom().getKey(), k -> new PlatformInfo()).recommended = origin
                        .getBom();
            }
        }

        if (providerInfo.isEmpty()) {
            log.info("No Quarkus platform BOMs found");
        } else {
            log.info("Quarkus platform BOMs:");
            boolean recommendExtraImports = false;
            for (PlatformInfo platform : providerInfo.values()) {
                if (platform.imported == null) {
                    recommendExtraImports = true;
                    continue;
                }
                final StringBuilder sb = new StringBuilder();
                if (platform.recommended == null) {
                    if (rectify) {
                        sb.append(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT,
                                UpdateCommandHandler.REMOVE, platform.imported.toCompactCoords()));
                        recommendationsAvailable = true;
                    } else {
                        sb.append("  ");
                        sb.append(platform.imported.toCompactCoords());
                        if (!projectState.getExtensions().isEmpty()) {
                            // The extension check is for modules that are aggregating modules (e.g. parent POMs)
                            // that import common BOMs. It's however not how it should be done.
                            sb.append(" | unnecessary");
                            recommendationsAvailable = true;
                        }
                    }
                } else if (platform.isVersionUpdateRecommended()) {
                    if (rectify) {
                        sb.append(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT,
                                UpdateCommandHandler.UPDATE, platform.imported.toCompactCoords()));
                        sb.append(platform.imported.toCompactCoords()).append(" -> ")
                                .append(platform.getRecommendedVersion());
                    } else {
                        sb.append("  ");
                        sb.append(platform.imported.toCompactCoords()).append(" | misaligned");
                    }
                    recommendationsAvailable = true;
                } else {
                    if (rectify) {
                        sb.append(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT, "",
                                platform.imported.toCompactCoords()));
                    } else {
                        sb.append("  ").append(platform.imported.toCompactCoords());
                    }
                }
                log.info(sb.toString());
            }
            if (rectify && recommendExtraImports) {
                for (PlatformInfo platform : providerInfo.values()) {
                    if (platform.imported == null) {
                        log.info(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT, UpdateCommandHandler.ADD,
                                platform.recommended.toCompactCoords()));
                    }
                }
                recommendationsAvailable = true;
            }
        }

        if (projectState.getExtensions().isEmpty()) {
            log.info("");
            log.info("No Quarkus extensions found among the project dependencies");
            return recommendationsAvailable;
        }

        log.info("");

        if (perModule) {
            final ModuleState mainModule = projectState.getMainModule();
            final Path baseDir = mainModule.getModuleDir();
            recommendationsAvailable |= logModuleInfo(projectState, mainModule, baseDir, log, rectify);
            for (ModuleState module : projectState.getModules()) {
                if (!module.isMain()) {
                    recommendationsAvailable |= logModuleInfo(projectState, module, baseDir, log, rectify);
                }
            }
        } else {
            for (ExtensionProvider provider : projectState.getExtensionProviders()) {
                if (provider.isPlatform()) {
                    recommendationsAvailable = logProvidedExtensions(provider, rectify, log, recommendationsAvailable);
                }
            }
            for (ExtensionProvider provider : projectState.getExtensionProviders()) {
                if (!provider.isPlatform()) {
                    recommendationsAvailable = logProvidedExtensions(provider, rectify, log, recommendationsAvailable);
                }
            }
        }
        return recommendationsAvailable;
    }

    private static boolean logProvidedExtensions(ExtensionProvider provider, boolean rectify, MessageWriter log,
            boolean recommendationsAvailable) {
        if (provider.getExtensions().isEmpty()) {
            return recommendationsAvailable;
        }
        log.info("Extensions from " + provider.getKey() + ":");
        final StringBuilder sb = new StringBuilder();
        for (TopExtensionDependency dep : provider.getExtensions()) {
            sb.setLength(0);
            recommendationsAvailable = logExtensionInfo(dep, rectify, sb, recommendationsAvailable);
            log.info(sb.toString());
        }
        log.info("");
        return recommendationsAvailable;
    }

    private static boolean logExtensionInfo(TopExtensionDependency dep, boolean rectify, StringBuilder sb,
            boolean recommendationsAvailable) {
        if (dep.isPlatformExtension()) {
            if (rectify) {
                if (dep.isNonRecommendedVersion()) {
                    sb.append(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT,
                            UpdateCommandHandler.UPDATE, ""));
                } else {
                    sb.append(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT, "", ""));
                }
                sb.append(dep.getArtifact().getGroupId()).append(':')
                        .append(dep.getArtifact().getArtifactId());
                if (!dep.getArtifact().getClassifier().isEmpty()) {
                    sb.append(':').append(dep.getArtifact().getClassifier());
                }
                if (dep.isNonRecommendedVersion()) {
                    sb.append(':').append(dep.getArtifact().getVersion());
                    if (rectify) {
                        sb.append(" -> remove version (managed)");
                    }
                    recommendationsAvailable = true;
                }
            } else {
                sb.append("  ").append(dep.getArtifact().getGroupId()).append(':')
                        .append(dep.getArtifact().getArtifactId());
                if (!dep.getArtifact().getClassifier().isEmpty()) {
                    sb.append(':').append(dep.getArtifact().getClassifier());
                }
                if (dep.isNonRecommendedVersion()) {
                    sb.append(':').append(dep.getArtifact().getVersion());
                    sb.append(" | misaligned");
                    recommendationsAvailable = true;
                }
            }
        } else {
            if (rectify) {
                sb.append(String.format(UpdateCommandHandler.PLATFORM_RECTIFY_FORMAT, "", ""));
            } else {
                sb.append("  ");
            }
            sb.append(dep.getArtifact().toCompactCoords());
        }
        if (dep.isTransitive()) {
            sb.append(" | transitive");
        }
        return recommendationsAvailable;
    }

    private static boolean logModuleInfo(ProjectState project, ModuleState module, Path baseDir, MessageWriter log,
            boolean rectify) {
        if (module.getExtensions().isEmpty() && module.getPlatformBoms().isEmpty() && !module.isMain()) {
            return false;
        }
        boolean recommendationsAvailable = false;

        final StringBuilder sb = new StringBuilder();
        if (module.isMain()) {
            sb.append("Main application module ");
        } else {
            sb.append("Module ");
        }
        sb.append(module.getId().getGroupId()).append(':').append(module.getId().getArtifactId()).append(':');
        log.info(sb.toString());

        final Iterator<Path> i = module.getWorkspaceModule().getBuildFiles().iterator();
        if (i.hasNext()) {
            sb.setLength(0);
            sb.append("  Build file: ");
            sb.append(baseDir.relativize(i.next()));
            while (i.hasNext()) {
                sb.append(", ").append(baseDir.relativize(i.next()));
            }
            log.info(sb.toString());
        }

        if (!module.getPlatformBoms().isEmpty()) {
            log.info("  Platform BOMs:");
            for (ArtifactCoords bom : module.getPlatformBoms()) {
                log.info("    " + bom.toCompactCoords());
            }
        }

        if (!module.getExtensions().isEmpty()) {
            final Map<String, List<TopExtensionDependency>> extDepsByProvider = new LinkedHashMap<>();
            for (TopExtensionDependency dep : module.getExtensions()) {
                extDepsByProvider.computeIfAbsent(dep.getProviderKey(), k -> new ArrayList<>()).add(dep);
            }
            for (ExtensionProvider provider : project.getExtensionProviders()) {
                if (!provider.isPlatform()) {
                    continue;
                }
                final List<TopExtensionDependency> extList = extDepsByProvider.getOrDefault(provider.getKey(),
                        Collections.emptyList());
                if (!extList.isEmpty()) {
                    log.info("  Extensions from " + provider.getKey() + ":");
                    for (TopExtensionDependency dep : extList) {
                        sb.setLength(0);
                        sb.append("  ");
                        recommendationsAvailable = logExtensionInfo(dep, rectify, sb, recommendationsAvailable);
                        log.info(sb.toString());
                    }
                    log.info("");
                }
            }
            for (ExtensionProvider provider : project.getExtensionProviders()) {
                if (provider.isPlatform()) {
                    continue;
                }
                final List<TopExtensionDependency> extList = extDepsByProvider.getOrDefault(provider.getKey(),
                        Collections.emptyList());
                if (!extList.isEmpty()) {
                    log.info("  Extensions from " + provider.getKey() + ":");
                    for (TopExtensionDependency dep : extList) {
                        sb.setLength(0);
                        sb.append("  ");
                        recommendationsAvailable = logExtensionInfo(dep, rectify, sb, recommendationsAvailable);
                        log.info(sb.toString());
                    }
                    log.info("");
                }
            }
        }
        return recommendationsAvailable;
    }

    protected static ProjectState resolveProjectState(ApplicationModel appModel, ExtensionCatalog currentCatalog) {
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

    static class PlatformInfo {
        ArtifactCoords imported;
        ArtifactCoords recommended;

        boolean isVersionUpdateRecommended() {
            return imported != null && recommended != null && !imported.getVersion().equals(recommended.getVersion());
        }

        String getRecommendedVersion() {
            return recommended == null ? null : recommended.getVersion();
        }

        boolean isImported() {
            return imported != null;
        }

        boolean isToBeImported() {
            return imported == null && recommended != null;
        }

        ArtifactCoords getRecommendedCoords() {
            return recommended == null ? imported : recommended;
        }

        String getRecommendedProviderKey() {
            if (recommended != null) {
                return ExtensionProvider.key(recommended, true);
            }
            return ExtensionProvider.key(imported, true);
        }
    }
}

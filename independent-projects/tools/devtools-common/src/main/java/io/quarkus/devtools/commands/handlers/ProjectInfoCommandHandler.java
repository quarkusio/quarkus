package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.messagewriter.MessageIcons.OUT_OF_DATE_ICON;
import static io.quarkus.devtools.messagewriter.MessageIcons.UP_TO_DATE_ICON;
import static io.quarkus.devtools.project.state.ProjectStates.resolveProjectState;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.devtools.commands.ProjectInfo;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.state.ExtensionProvider;
import io.quarkus.devtools.project.state.ModuleState;
import io.quarkus.devtools.project.state.ProjectState;
import io.quarkus.devtools.project.state.TopExtensionDependency;
import io.quarkus.devtools.project.update.PlatformInfo;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.registry.catalog.ExtensionOrigin;

public class ProjectInfoCommandHandler implements QuarkusCommandHandler {

    public static final String RECOMMENDATIONS_AVAILABLE = "recommendations-available";

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {

        final ApplicationModel appModel = invocation.getValue(ProjectInfo.APP_MODEL);
        final boolean logStatePerModule = invocation.getValue(ProjectInfo.PER_MODULE, false);

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
            providerInfo.computeIfAbsent(bom.getKey(), k -> new PlatformInfo(bom, null));
        }
        for (TopExtensionDependency dep : projectState.getExtensions()) {
            final ExtensionOrigin origin = dep.getOrigin();
            if (origin != null && origin.isPlatform()) {
                providerInfo.compute(origin.getBom().getKey(), (k, v) -> {
                    if (v == null) {
                        return new PlatformInfo(null, origin.getBom());
                    }
                    return new PlatformInfo(v.getImported(), origin.getBom());
                });
            }
        }

        if (providerInfo.isEmpty()) {
            log.info("No Quarkus platform BOMs found");
        } else {
            log.info("Quarkus platform BOMs:");
            boolean recommendExtraImports = false;
            for (PlatformInfo platform : providerInfo.values()) {
                if (!platform.isImported()) {
                    recommendExtraImports = true;
                    continue;
                }
                final StringBuilder sb = new StringBuilder();
                if (platform.getRecommended() == null) {
                    if (rectify) {
                        sb.append(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                UpdateProjectCommandHandler.REMOVE, platform.getImported().toCompactCoords()));
                        recommendationsAvailable = true;
                    } else {
                        sb.append("  ");
                        sb.append(platform.getImported().toCompactCoords());
                        if (!projectState.getExtensions().isEmpty()) {
                            // The extension check is for modules that are aggregating modules (e.g. parent POMs)
                            // that import common BOMs. It's however not how it should be done.
                            sb.append(" | unnecessary");
                            recommendationsAvailable = true;
                        }
                    }
                } else if (platform.isVersionUpdateRecommended()) {
                    sb.append(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            rectify ? UpdateProjectCommandHandler.UPDATE : "", platform.getImported().toCompactCoords()));
                    if (rectify) {
                        sb.append(platform.getImported().toCompactCoords()).append(" -> ")
                                .append(platform.getRecommendedVersion());
                    } else {
                        sb.append(" ").append(OUT_OF_DATE_ICON.iconOrMessage());
                    }
                    recommendationsAvailable = true;
                } else {
                    sb.append(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "",
                            platform.getImported().toCompactCoords()))
                            .append(" ").append(UP_TO_DATE_ICON.iconOrMessage());
                }
                log.info(sb.toString());
            }
            if (rectify && recommendExtraImports) {
                for (PlatformInfo platform : providerInfo.values()) {
                    if (platform.getImported() == null) {
                        log.info(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                                UpdateProjectCommandHandler.ADD,
                                platform.getRecommended().toCompactCoords()));
                    }
                }
                recommendationsAvailable = true;
            }
        }

        if (projectState.getExtensions().isEmpty()) {
            log.info("");
            log.info("No Quarkus extensions were found among the project dependencies");
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
                    sb.append(String.format(UpdateProjectCommandHandler.ITEM_FORMAT,
                            UpdateProjectCommandHandler.UPDATE, ""));
                } else {
                    sb.append(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "", ""));
                }
                sb.append(dep.getArtifact().getGroupId()).append(':')
                        .append(dep.getArtifact().getArtifactId());
                if (!dep.getArtifact().getClassifier().isEmpty()) {
                    sb.append(':').append(dep.getArtifact().getClassifier());
                }
                if (dep.isNonRecommendedVersion()) {
                    sb.append(':').append(dep.getArtifact().getVersion());
                    if (rectify) {
                        sb.append(" -> drop version (managed by platform)");
                    }
                    recommendationsAvailable = true;
                } else {
                    sb.append(" ").append(UP_TO_DATE_ICON.iconOrMessage());
                    ;
                }
            } else {
                sb.append(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "", "")).append(dep.getArtifact().getGroupId())
                        .append(':')
                        .append(dep.getArtifact().getArtifactId());
                if (!dep.getArtifact().getClassifier().isEmpty()) {
                    sb.append(':').append(dep.getArtifact().getClassifier());
                }
                if (dep.isNonRecommendedVersion()) {
                    sb.append(':').append(dep.getArtifact().getVersion());
                    sb.append(" ").append(OUT_OF_DATE_ICON.iconOrMessage());
                    recommendationsAvailable = true;
                } else {
                    sb.append(" ").append(UP_TO_DATE_ICON.iconOrMessage());
                    ;
                }
            }
        } else {
            sb.append(String.format(UpdateProjectCommandHandler.ITEM_FORMAT, "", ""));
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

}

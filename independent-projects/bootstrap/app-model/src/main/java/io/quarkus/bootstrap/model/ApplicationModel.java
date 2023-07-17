package io.quarkus.bootstrap.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Application dependency model. Allows to explore application dependencies,
 * Quarkus platforms found in the project configuration and Quarkus platform configuration properties.
 */
public interface ApplicationModel {

    /**
     * Main application artifact
     *
     * @return main application artifact
     */
    ResolvedDependency getAppArtifact();

    /**
     * All the dependencies of an application including runtime and build time dependencies.
     *
     * @return application runtime and build time dependencies
     */
    Collection<ResolvedDependency> getDependencies();

    /**
     * Runtime dependencies of an application
     *
     * @return runtime dependencies of an application
     */
    default Collection<ResolvedDependency> getRuntimeDependencies() {
        return getDependencies().stream().filter(Dependency::isRuntimeCp).collect(Collectors.toList());
    }

    /**
     * Quarkus platforms (BOMs) found in the configuration of an application
     *
     * @return Quarkus platforms (BOMs) found in the configuration of an application
     */
    PlatformImports getPlatforms();

    /**
     * Quarkus platform configuration properties
     *
     * @return Quarkus platform configuration properties
     */
    default Map<String, String> getPlatformProperties() {
        final PlatformImports platformImports = getPlatforms();
        return platformImports == null ? Map.of() : platformImports.getPlatformProperties();
    }

    /**
     * Extension capability requirements collected from the extensions found on the classpath of an application
     *
     * @return Extension capability requirements collected from the extensions found on the classpath of an application
     */
    Collection<ExtensionCapabilities> getExtensionCapabilities();

    /**
     * Class loading parent-first artifacts
     *
     * @return class loading parent-first artifacts
     */
    Set<ArtifactKey> getParentFirst();

    /**
     * Class loading runner parent-first artifacts
     *
     * @return class loading runner parent-first artifacts
     */
    Set<ArtifactKey> getRunnerParentFirst();

    /**
     * Class loading lower priority artifacts
     *
     * @return class loading lower priority artifacts
     */
    Set<ArtifactKey> getLowerPriorityArtifacts();

    /**
     * Local project dependencies that are live-reloadable in dev mode.
     *
     * @return local project dependencies that are live-reloadable in dev mode.
     */
    Set<ArtifactKey> getReloadableWorkspaceDependencies();

    /**
     * Resources that should be removed from the classpath.
     *
     * @return resources that should be removed from the classpath
     */
    Map<ArtifactKey, Set<String>> getRemovedResources();

    /**
     * Main workspace module of an application. Could be null, in case the project is not available during the build.
     *
     * @return main workspace module of an application, could be null, in case the project is not available during the build
     */
    default WorkspaceModule getApplicationModule() {
        return getAppArtifact().getWorkspaceModule();
    }

    /**
     * All the workspace modules found as dependencies of an application
     *
     * @return all the workspace modules found as dependencies of an application
     */
    default Collection<WorkspaceModule> getWorkspaceModules() {
        final Map<WorkspaceModuleId, WorkspaceModule> result = new HashMap<>();
        collectModules(getAppArtifact().getWorkspaceModule(), result);
        for (ResolvedDependency d : getDependencies()) {
            collectModules(d.getWorkspaceModule(), result);
        }
        return result.values();
    }

    private static void collectModules(WorkspaceModule module, Map<WorkspaceModuleId, WorkspaceModule> collected) {
        if (module == null) {
            return;
        }
        collected.putIfAbsent(module.getId(), module);

        WorkspaceModule parent = module.getParent();
        if (parent != null) {
            collectModules(parent, collected);
        }

        for (Dependency d : module.getDirectDependencyConstraints()) {
            if (!Dependency.SCOPE_IMPORT.equals(d.getScope())
                    || !(d instanceof ResolvedDependency)) {
                continue;
            }
            collectModules(((ResolvedDependency) d).getWorkspaceModule(), collected);
        }
    }
}

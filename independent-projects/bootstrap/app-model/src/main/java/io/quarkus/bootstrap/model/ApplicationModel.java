package io.quarkus.bootstrap.model;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface ApplicationModel {

    String PARENT_FIRST_ARTIFACTS = "parent-first-artifacts";
    String RUNNER_PARENT_FIRST_ARTIFACTS = "runner-parent-first-artifacts";
    String EXCLUDED_ARTIFACTS = "excluded-artifacts";
    String LESSER_PRIORITY_ARTIFACTS = "lesser-priority-artifacts";

    ResolvedDependency getAppArtifact();

    Collection<ResolvedDependency> getDependencies();

    default Collection<ResolvedDependency> getRuntimeDependencies() {
        return getDependencies().stream().filter(Dependency::isRuntimeCp).collect(Collectors.toList());
    }

    PlatformImports getPlatforms();

    default Map<String, String> getPlatformProperties() {
        final PlatformImports platformImports = getPlatforms();
        return platformImports == null ? Collections.emptyMap() : platformImports.getPlatformProperties();
    }

    Collection<ExtensionCapabilities> getExtensionCapabilities();

    Set<ArtifactKey> getParentFirst();

    Set<ArtifactKey> getRunnerParentFirst();

    Set<ArtifactKey> getLowerPriorityArtifacts();

    Set<ArtifactKey> getReloadableWorkspaceDependencies();

    /**
     * Resources that should be removed from the classpath.
     *
     * @return resources that should be removed from the classpath
     */
    Map<ArtifactKey, Set<String>> getRemovedResources();

    default WorkspaceModule getApplicationModule() {
        return getAppArtifact().getWorkspaceModule();
    }

    default Collection<WorkspaceModule> getWorkspaceModules() {
        final Map<WorkspaceModuleId, WorkspaceModule> modules = new HashMap<>();
        for (ResolvedDependency d : getDependencies()) {
            final WorkspaceModule module = d.getWorkspaceModule();
            if (module != null) {
                modules.putIfAbsent(module.getId(), module);
            }
        }
        return modules.values();
    }
}

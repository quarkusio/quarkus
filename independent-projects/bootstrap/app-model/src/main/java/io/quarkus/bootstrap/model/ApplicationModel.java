package io.quarkus.bootstrap.model;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

    default WorkspaceModule getApplicationModule() {
        return getAppArtifact().getWorkspaceModule();
    }

    default Collection<WorkspaceModule> getWorkspaceModules() {
        final List<WorkspaceModule> modules = new ArrayList<>();
        for (ResolvedDependency d : getDependencies()) {
            final WorkspaceModule module = d.getWorkspaceModule();
            if (module != null) {
                modules.add(module);
            }
        }
        return modules;
    }
}

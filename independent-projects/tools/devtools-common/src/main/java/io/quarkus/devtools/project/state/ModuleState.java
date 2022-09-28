package io.quarkus.devtools.project.state;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Represents a state of a module of a Quarkus project focusing primarily on Quarkus-specific information, such as
 * imported Quarkus platform BOMs, extensions and their origins.
 */
public class ModuleState {

    public class Builder {

        private Builder() {
        }

        public Builder setWorkspaceModule(WorkspaceModule module) {
            ModuleState.this.module = module;
            return this;
        }

        public Builder setMainModule(boolean main) {
            ModuleState.this.main = main;
            return this;
        }

        public Builder addPlatformBom(ArtifactCoords coords) {
            ModuleState.this.platformBomImports.add(coords);
            return this;
        }

        public Builder addExtensionDependency(TopExtensionDependency dep) {
            ModuleState.this.directExtDeps.put(dep.getKey(), dep);
            return this;
        }

        public ModuleState build() {
            return ModuleState.this;
        }
    }

    public static Builder builder() {
        return new ModuleState().new Builder();
    }

    private WorkspaceModule module;
    private final Set<ArtifactCoords> platformBomImports = new LinkedHashSet<>(1);
    private final Map<ArtifactKey, TopExtensionDependency> directExtDeps = new LinkedHashMap<>();
    private boolean main;

    private ModuleState() {
    }

    public WorkspaceModuleId getId() {
        return module.getId();
    }

    public WorkspaceModule getWorkspaceModule() {
        return module;
    }

    public Path getModuleDir() {
        return module.getModuleDir().toPath();
    }

    public boolean isMain() {
        return main;
    }

    public Collection<ArtifactCoords> getPlatformBoms() {
        return platformBomImports;
    }

    public Collection<TopExtensionDependency> getExtensions() {
        return directExtDeps.values();
    }

    public boolean hasExtension(ArtifactKey key) {
        return directExtDeps.containsKey(key);
    }
}

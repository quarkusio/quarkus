package io.quarkus.devtools.project.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Represents a Quarkus project state focusing primarily on Quarkus-specific information, such as
 * imported Quarkus platform BOMs, extensions and their origins.
 */
public class ProjectState {

    public class Builder {

        private Builder() {
        }

        public Builder addPlatformBom(ArtifactCoords coords) {
            ProjectState.this.importedPlatformBoms.add(coords);
            return this;
        }

        public Builder addExtensionDependency(TopExtensionDependency dep) {
            ProjectState.this.topExtensions.put(dep.getKey(), dep);
            return this;
        }

        public Builder addModule(ModuleState state) {
            ProjectState.this.modules.put(state.getId(), state);
            if (state.isMain()) {
                ProjectState.this.mainModule = state;
            }
            return this;
        }

        public Builder addExtensionProvider(ExtensionProvider provider) {
            providers.put(provider.getKey(), provider);
            return this;
        }

        public ProjectState build() {
            return ProjectState.this;
        }
    }

    public static Builder builder() {
        return new ProjectState().new Builder();
    }

    private final List<ArtifactCoords> importedPlatformBoms = new ArrayList<>();
    private final Map<ArtifactKey, TopExtensionDependency> topExtensions = new LinkedHashMap<>();
    private ModuleState mainModule;
    private final Map<WorkspaceModuleId, ModuleState> modules = new LinkedHashMap<>();
    private final Map<String, ExtensionProvider> providers = new LinkedHashMap<>();

    public Collection<ArtifactCoords> getPlatformBoms() {
        return importedPlatformBoms;
    }

    public Collection<TopExtensionDependency> getExtensions() {
        return topExtensions.values();
    }

    public TopExtensionDependency getExtension(ArtifactKey key) {
        return topExtensions.get(key);
    }

    public Collection<ModuleState> getModules() {
        return modules.values();
    }

    public ModuleState getMainModule() {
        return mainModule;
    }

    public ModuleState getModule(WorkspaceModuleId id) {
        return modules.get(id);
    }

    public Collection<ExtensionProvider> getExtensionProviders() {
        return providers.values();
    }
}

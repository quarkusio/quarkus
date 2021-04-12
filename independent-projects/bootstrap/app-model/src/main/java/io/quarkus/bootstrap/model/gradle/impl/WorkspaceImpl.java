package io.quarkus.bootstrap.model.gradle.impl;

import io.quarkus.bootstrap.model.gradle.ArtifactCoords;
import io.quarkus.bootstrap.model.gradle.Workspace;
import io.quarkus.bootstrap.model.gradle.WorkspaceModule;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorkspaceImpl implements Workspace, Serializable {

    public ArtifactCoords mainModuleKey;
    public Map<ArtifactCoords, WorkspaceModule> modules = new HashMap<>();

    public WorkspaceImpl(ArtifactCoords mainModuleKey, Set<WorkspaceModule> workspaceModules) {
        this.mainModuleKey = mainModuleKey;
        for (WorkspaceModule module : workspaceModules) {
            modules.put(module.getArtifactCoords(), module);
        }
    }

    @Override
    public WorkspaceModule getMainModule() {
        return modules.get(mainModuleKey);
    }

    @Override
    public Collection<WorkspaceModule> getAllModules() {
        return modules.values();
    }

    @Override
    public WorkspaceModule getModule(ArtifactCoords key) {
        return modules.get(key);
    }

}

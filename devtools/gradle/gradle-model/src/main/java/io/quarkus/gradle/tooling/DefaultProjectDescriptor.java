package io.quarkus.gradle.tooling;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;

public class DefaultProjectDescriptor implements Serializable, ProjectDescriptor {

    private static final long serialVersionUID = 1L;

    private WorkspaceModule.Mutable module;

    /**
     * Workspace modules of the other projects of the build, keyed by project path
     */
    private final Map<String, WorkspaceModule.Mutable> projectModules;

    public DefaultProjectDescriptor(WorkspaceModule.Mutable module) {
        this(module, Map.of());
    }

    public DefaultProjectDescriptor(WorkspaceModule.Mutable module, Map<String, WorkspaceModule.Mutable> projectModules) {
        this.module = module;
        this.projectModules = new HashMap<>(projectModules);
    }

    @Override
    public WorkspaceModule.Mutable getWorkspaceModule() {
        return module;
    }

    public void setWorkspaceModule(WorkspaceModule.Mutable module) {
        this.module = module;
    }

    @Override
    public WorkspaceModule.Mutable getWorkspaceModuleOrNull(WorkspaceModuleId moduleId) {
        return module.getId().equals(moduleId) ? module : null;
    }

    @Override
    public WorkspaceModule.Mutable getWorkspaceModuleForProjectOrNull(String projectPath) {
        return projectModules.get(projectPath);
    }

    @Override
    public String toString() {
        return "DefaultProjectDescriptor{" +
                "\nmodule=" + module +
                "\nprojectModules=" + projectModules.keySet() +
                "\n}";
    }
}

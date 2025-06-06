package io.quarkus.gradle.tooling;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;

public interface ProjectDescriptor {

    /**
     * Project workspace module
     *
     * @return workspace module
     */
    WorkspaceModule.Mutable getWorkspaceModule();

    /**
     * Workspace module for a specific module ID (in a multi module project)
     *
     * @param moduleId module ID
     * @return workspace module for a given module ID or null, if the requested module info is not available
     */
    WorkspaceModule.Mutable getWorkspaceModuleOrNull(WorkspaceModuleId moduleId);
}

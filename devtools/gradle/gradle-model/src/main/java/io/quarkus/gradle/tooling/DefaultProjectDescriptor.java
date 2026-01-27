package io.quarkus.gradle.tooling;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.GAV;

public class DefaultProjectDescriptor implements Serializable, ProjectDescriptor {

    private static final long serialVersionUID = 1L;

    private WorkspaceModule.Mutable module;
    private final String projectPath;
    private final Map<String, List<DependencyInfoCollector.DeclaredDependency>> declaredDepsByProjectPath;
    private final Map<GAV, Model> pomModelsByGav;

    public DefaultProjectDescriptor(WorkspaceModule.Mutable module, String projectPath,
            Map<GAV, Model> pomModelsByGav,
            Map<String, List<DependencyInfoCollector.DeclaredDependency>> declaredDepsByProjectPath
    //            Map<String, List<DependencyInfoCollector.DeclaredDependency>> declaredDepsByProjectPath,
    //            Map<GAV, File> pomFilesByGav
    ) {
        this.module = module;
        this.projectPath = projectPath;
        this.declaredDepsByProjectPath = declaredDepsByProjectPath;
        this.pomModelsByGav = pomModelsByGav;
    }

    @Override
    public WorkspaceModule.Mutable getWorkspaceModule() {
        return module;
    }

    public void setWorkspaceModule(WorkspaceModule.Mutable module) {
        this.module = module;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public Map<String, List<DependencyInfoCollector.DeclaredDependency>> getDeclaredDepsByProjectPath() {
        return declaredDepsByProjectPath;
    }

    // public Map<GAV, File> getPomFilesByGav() {
    //     return pomFilesByGav;
    // }

    public Map<GAV, Model> getPomModelByGav() {
        return pomModelsByGav;
    }

    @Override
    public WorkspaceModule.Mutable getWorkspaceModuleOrNull(WorkspaceModuleId moduleId) {
        return module.getId().equals(moduleId) ? module : null;
    }

    @Override
    public String toString() {
        return "DefaultProjectDescriptor{" +
                "\nmodule=" + module +
                "\n}";
    }
}

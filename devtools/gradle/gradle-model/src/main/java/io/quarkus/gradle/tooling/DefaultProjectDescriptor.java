package io.quarkus.gradle.tooling;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.paths.PathCollection;

public class DefaultProjectDescriptor implements Serializable, ProjectDescriptor {

    private static final long serialVersionUID = 1L;

    private WorkspaceModule.Mutable module;

    public DefaultProjectDescriptor(WorkspaceModule.Mutable module) {
        this.module = module;
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

    /**
     * Returns a copy of this descriptor with its own workspace module. Tasks call this before changing the module,
     * because without the configuration cache every application model task of the project gets the same descriptor.
     */
    public DefaultProjectDescriptor copy() {
        return new DefaultProjectDescriptor(copyModule());
    }

    /**
     * Copies the workspace module field by field: {@link WorkspaceModule#mutable()} returns the same instance, and
     * {@code build().mutable()} and {@link WorkspaceModule.Mutable#fromMap(java.util.Map)} drop the parent.
     */
    private WorkspaceModule.Mutable copyModule() {
        final WorkspaceModule.Mutable copy = WorkspaceModule.builder().setModuleId(module.getId());
        if (module.getModuleDir() != null) {
            copy.setModuleDir(module.getModuleDir().toPath());
        }
        if (module.getBuildDir() != null) {
            copy.setBuildDir(module.getBuildDir().toPath());
        }
        final PathCollection buildFiles = module.getBuildFiles();
        if (!buildFiles.isEmpty()) {
            final Iterator<Path> i = buildFiles.iterator();
            copy.setBuildFile(i.next());
            if (i.hasNext()) {
                // WorkspaceModule.Mutable can only set one build file, which is all a Gradle project has
                throw new IllegalArgumentException(
                        "Cannot copy workspace module " + module.getId() + " with several build files: " + buildFiles);
            }
        }
        for (String classifier : module.getSourceClassifiers()) {
            copy.addArtifactSources(module.getSources(classifier));
        }
        if (!module.getDirectDependencyConstraints().isEmpty()) {
            copy.setDependencyConstraints(new ArrayList<>(module.getDirectDependencyConstraints()));
        }
        if (!module.getDirectDependencies().isEmpty()) {
            copy.setDependencies(new ArrayList<>(module.getDirectDependencies()));
        }
        if (!module.getTestClasspathDependencyExclusions().isEmpty()) {
            copy.setTestClasspathDependencyExclusions(new ArrayList<>(module.getTestClasspathDependencyExclusions()));
        }
        if (!module.getAdditionalTestClasspathElements().isEmpty()) {
            copy.setAdditionalTestClasspathElements(new ArrayList<>(module.getAdditionalTestClasspathElements()));
        }
        if (module.getParent() != null) {
            copy.setParent(module.getParent());
        }
        return copy;
    }

    @Override
    public String toString() {
        return "DefaultProjectDescriptor{" +
                "\nmodule=" + module +
                "\n}";
    }
}

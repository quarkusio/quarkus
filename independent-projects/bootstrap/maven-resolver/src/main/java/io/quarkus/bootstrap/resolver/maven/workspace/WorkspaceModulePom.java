package io.quarkus.bootstrap.resolver.maven.workspace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

public class WorkspaceModulePom {

    // new, not yet loaded, state
    private static int STATE_NEW = 0;
    // model has been loaded but not yet processed (added to the workspace)
    private static int STATE_LOADED = 1;
    // module has been added to the workspace
    private static int STATE_PROCESSED = 2;

    final Path pom;
    Model model;
    Model effectiveModel;
    WorkspaceModulePom parent;
    int state = STATE_NEW;
    // a queue of modules that should be loaded after this one
    final ConcurrentLinkedDeque<WorkspaceModulePom> thenLoad = new ConcurrentLinkedDeque<>();

    WorkspaceModulePom(Path pom) {
        this(pom, null, null);
    }

    public WorkspaceModulePom(Path pom, Model model, Model effectiveModel) {
        this.pom = pom.normalize().toAbsolutePath();
        this.model = model;
        this.effectiveModel = effectiveModel;
    }

    Path getModuleDir() {
        var moduleDir = pom.getParent();
        return moduleDir == null ? WorkspaceLoader.getFsRootDir() : moduleDir;
    }

    Model getModel() {
        return model == null ? model = WorkspaceLoader.readModel(pom) : model;
    }

    boolean isParentConfigured() {
        return getModel().getParent() != null;
    }

    Path getParentPom() {
        Path parentPom = null;
        final Parent parent = getModel().getParent();
        if (parent != null && parent.getRelativePath() != null && !parent.getRelativePath().isEmpty()) {
            parentPom = pom.getParent().resolve(parent.getRelativePath()).normalize();
            if (Files.isDirectory(parentPom)) {
                parentPom = parentPom.resolve(WorkspaceLoader.POM_XML);
            }
        } else {
            final Path parentDir = pom.getParent().getParent();
            if (parentDir != null) {
                parentPom = parentDir.resolve(WorkspaceLoader.POM_XML);
            }
        }
        return parentPom != null && Files.exists(parentPom) ? parentPom.normalize().toAbsolutePath() : null;
    }

    boolean isNew() {
        return state == STATE_NEW;
    }

    boolean isLoaded() {
        return state == STATE_LOADED;
    }

    void setLoaded() {
        state = STATE_LOADED;
    }

    void process(Consumer<WorkspaceModulePom> processor) {
        if (state == STATE_PROCESSED) {
            return;
        }
        state = STATE_PROCESSED;
        if (parent != null) {
            parent.process(processor);
        }
        if (model != null && model != WorkspaceLoader.MISSING_MODEL) {
            processor.accept(this);
        }
    }

    /**
     * Group Ids aren't always configured in a pom.xml.
     * Sometimes, they are inherited from a parent POM.
     * This method returns resolved groupId value for this module.
     *
     * @return resolved groupId for this module
     */
    String getResolvedGroupId() {
        if (effectiveModel != null) {
            return effectiveModel.getGroupId();
        }
        final Model model = getModel();
        if (model != null) {
            String groupId = model.getGroupId();
            if (groupId != null) {
                return groupId;
            }
            Parent parent = model.getParent();
            if (parent != null) {
                groupId = parent.getGroupId();
                if (groupId != null) {
                    return groupId;
                }
            }
        }
        if (parent != null) {
            return parent.getResolvedGroupId();
        }
        throw new RuntimeException("Failed to determine the groupId of module " + pom);
    }

    /**
     * Returns a resolved version value for this module, which may be inherited from a parent POM.
     *
     * @return resolved version value for this module
     */
    String getResolvedVersion() {
        if (effectiveModel != null) {
            return effectiveModel.getVersion();
        }
        final Model model = getModel();
        if (model != null) {
            String version = ModelUtils.getRawVersionOrNull(model);
            if (version != null && ModelUtils.isUnresolvedVersion(version)) {
                version = ModelUtils.resolveVersion(version, model);
            }
            if (version != null) {
                return version;
            }
        }
        if (parent != null) {
            return parent.getResolvedVersion();
        }
        throw new RuntimeException("Failed to determine the version of module " + pom);
    }

    /**
     * Allows scheduling module loading after this module has been loaded.
     *
     * @param module module to load once this module has been loaded
     */
    void thenLoad(WorkspaceModulePom module) {
        thenLoad.add(module);
    }

    /**
     * Modules that should be loaded after this module.
     *
     * @return modules that should be loaded after this module
     */
    Deque<WorkspaceModulePom> getThenLoad() {
        return thenLoad;
    }

    @Override
    public String toString() {
        return String.valueOf(pom);
    }
}

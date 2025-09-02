package io.quarkus.bootstrap.resolver.maven.workspace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

public class WorkspaceModulePom {
    final Path pom;
    Model model;
    Model effectiveModel;
    WorkspaceModulePom parent;
    boolean processed;

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

    Path getParentPom() {
        if (model == null) {
            return null;
        }
        Path parentPom = null;
        final Parent parent = model.getParent();
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
        return parentPom != null && Files.exists(parentPom) ? parentPom : null;
    }

    void process(Consumer<WorkspaceModulePom> consumer) {
        if (processed) {
            return;
        }
        processed = true;
        if (parent != null) {
            parent.process(consumer);
        }
        if (model != null && model != WorkspaceLoader.MISSING_MODEL) {
            consumer.accept(this);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(pom);
    }
}

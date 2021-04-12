package io.quarkus.bootstrap.model.gradle.impl;

import io.quarkus.bootstrap.model.gradle.ArtifactCoords;
import io.quarkus.bootstrap.model.gradle.SourceSet;
import io.quarkus.bootstrap.model.gradle.WorkspaceModule;
import java.io.File;
import java.io.Serializable;
import java.util.Objects;

public class WorkspaceModuleImpl implements WorkspaceModule, Serializable {

    private final ArtifactCoords artifactCoords;
    private final File projectRoot;
    private final File buildDir;
    private final SourceSet sourceSourceSet;
    private final SourceSet sourceSet;

    public WorkspaceModuleImpl(ArtifactCoords artifactCoords, File projectRoot, File buildDir, SourceSet sourceSourceSet,
            SourceSet sourceSet) {
        this.artifactCoords = artifactCoords;
        this.projectRoot = projectRoot;
        this.buildDir = buildDir;
        this.sourceSourceSet = sourceSourceSet;
        this.sourceSet = sourceSet;
    }

    @Override
    public ArtifactCoords getArtifactCoords() {
        return artifactCoords;
    }

    @Override
    public File getProjectRoot() {
        return projectRoot;
    }

    @Override
    public File getBuildDir() {
        return buildDir;
    }

    @Override
    public SourceSet getSourceSet() {
        return sourceSet;
    }

    @Override
    public SourceSet getSourceSourceSet() {
        return sourceSourceSet;
    }

    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        WorkspaceModuleImpl that = (WorkspaceModuleImpl) object;
        return java.util.Objects.equals(artifactCoords, that.artifactCoords) &&
                java.util.Objects.equals(projectRoot, that.projectRoot) &&
                java.util.Objects.equals(buildDir, that.buildDir) &&
                java.util.Objects.equals(sourceSourceSet, that.sourceSourceSet) &&
                java.util.Objects.equals(sourceSet, that.sourceSet);
    }

    public int hashCode() {
        return Objects.hash(super.hashCode(), artifactCoords, projectRoot, buildDir, sourceSourceSet, sourceSet);
    }
}

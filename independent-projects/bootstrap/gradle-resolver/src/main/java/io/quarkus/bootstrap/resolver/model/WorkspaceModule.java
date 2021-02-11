package io.quarkus.bootstrap.resolver.model;

import java.io.File;

public interface WorkspaceModule {

    ArtifactCoords getArtifactCoords();

    File getProjectRoot();

    File getBuildDir();

    SourceSet getSourceSet();

    SourceSet getSourceSourceSet();
}

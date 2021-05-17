package io.quarkus.bootstrap.model.gradle;

import io.quarkus.bootstrap.model.PathsCollection;

public interface SourceSet {

    PathsCollection getSourceDirectories();

    PathsCollection getResourceDirectories();
}

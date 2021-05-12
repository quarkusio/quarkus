package io.quarkus.bootstrap.model.gradle;

import java.io.File;
import java.util.Set;

public interface SourceSet {

    Set<File> getSourceDirectories();

    Set<File> getResourceDirectories();
}

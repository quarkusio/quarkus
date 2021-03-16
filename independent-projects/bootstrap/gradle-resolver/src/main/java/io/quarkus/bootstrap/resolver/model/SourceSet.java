package io.quarkus.bootstrap.resolver.model;

import java.io.File;
import java.util.Set;

public interface SourceSet {

    Set<File> getSourceDirectories();

    File getResourceDirectory();
}

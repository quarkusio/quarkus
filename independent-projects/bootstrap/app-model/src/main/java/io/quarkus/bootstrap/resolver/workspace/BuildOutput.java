package io.quarkus.bootstrap.resolver.workspace;

import io.quarkus.bootstrap.model.PathsCollection;
import java.nio.file.Path;

/**
 * Represents an outcome of the build for a specific set of sources and resources.
 */
public interface BuildOutput {

    Path getOutputDir();

    PathsCollection getSourcesDirs();

    PathsCollection getResourcesDirs();
}

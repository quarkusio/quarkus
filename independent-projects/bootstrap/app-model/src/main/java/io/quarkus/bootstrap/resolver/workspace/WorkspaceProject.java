package io.quarkus.bootstrap.resolver.workspace;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Represents a project in a workspace
 */
public interface WorkspaceProject {

    String getGroupId();

    String getArtifactId();

    String getVersion();

    /**
     * Project location on the filesystem
     *
     * @return project location
     */
    Path getDir();

    /**
     * Project's key consisting of its group and artifact IDs
     *
     * @return project's key
     */
    AppArtifactKey getKey();

    /**
     * Where the main build output can be located and from which
     * sources and resources it was produced
     *
     * @return main build output
     */
    Collection<BuildOutput> getMainOutput();

    /**
     * Where the test build output can be located and from which
     * sources and resources it was produced
     *
     * @return test build output
     */
    Collection<BuildOutput> getTestOutput();

    default AppArtifact getAppArtifact(String extension) {
        return getAppArtifact("", extension);
    }

    default AppArtifact getAppArtifact(String classifier, String extension) {
        return new AppArtifact(getGroupId(), getArtifactId(), classifier, extension, getVersion());
    }
}

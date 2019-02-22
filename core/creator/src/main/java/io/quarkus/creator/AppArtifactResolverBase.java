/**
 *
 */
package io.quarkus.creator;

import java.nio.file.Path;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class AppArtifactResolverBase implements AppArtifactResolver {

    @Override
    public Path resolve(AppArtifact artifact) throws AppCreatorException {
        Path path = artifact.getPath();
        if (path != null) {
            return path;
        }
        doResolve(artifact);
        path = artifact.getPath();
        if (path == null) {
            throw new AppCreatorException("Failed to resolve " + artifact);
        }
        return path;
    }

    protected static void setPath(AppArtifact artifact, Path p) {
        artifact.setPath(p);
    }

    protected abstract void doResolve(AppArtifact artifact) throws AppCreatorException;
}

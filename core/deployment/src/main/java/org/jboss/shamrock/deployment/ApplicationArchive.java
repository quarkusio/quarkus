package org.jboss.shamrock.deployment;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.jandex.IndexView;

/**
 * Represents an archive that is part of application code.
 * <p>
 * An application archive is an archive that provides components to the application. As a result it will be indexed
 * via jandex, and any deployment descriptors will be made available to the application.
 */
public interface ApplicationArchive extends Closeable {

    /**
     *
     * @return The index of this application Archive
     */
    IndexView getIndex();

    /**
     *
     * @return The archive root.
     */
    Path getArchiveRoot();

    /**
     * Convenience method, returns the child path if it exists, otherwise null.
     *
     * @param path The child path
     * @return The child path, or null if it does not exist.
     */
    default Path getChildPath(String path) {
        Path result = getArchiveRoot().resolve(path);
        if (Files.exists(result)) {
            return result;
        }
        return null;
    }

}

package io.quarkus.deployment.dev.filewatch;

import java.util.Collection;

/**
 * Callback for file system change events
 *
 */
public interface FileChangeCallback {

    /**
     * Method that is invoked when file system changes are detected.
     *
     * @param changes the file system changes
     */
    void handleChanges(final Collection<FileChangeEvent> changes);

}

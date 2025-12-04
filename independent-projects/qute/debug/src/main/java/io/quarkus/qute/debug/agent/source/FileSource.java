package io.quarkus.qute.debug.agent.source;

import java.net.URI;
import java.nio.file.Path;

/**
 * A {@link RemoteSource} representing a Qute template stored as a local file.
 * <p>
 * This implementation is used when the template resides directly on the file
 * system (as opposed to being embedded inside a JAR). It sets the DAP
 * {@code path} field so that the client can open and display the file.
 * </p>
 *
 * <p>
 * Example:
 * </p>
 *
 * <pre>
 * file:///home/user/project/src/main/resources/templates/index.html
 * </pre>
 */
public class FileSource extends RemoteSource {

    public FileSource(URI uri, String templateId) {
        this(uri, templateId, 0);
    }

    /**
     * Creates a new {@link FileSource} for a Qute template located on the local filesystem.
     *
     * @param uri the URI of the template file (must use the {@code file:} scheme)
     * @param templateId the Qute template identifier
     */
    protected FileSource(URI uri, String templateId, int startLine) {
        super(uri, templateId, startLine);

        // Initialize the DAP "path" field so the client can open the file.
        try {
            super.setPath(Path.of(uri).toString());
        } catch (Exception e) {
            // Fallback to ASCII representation if the URI cannot be converted to a Path
            super.setPath(uri != null ? uri.toASCIIString() : templateId);
        }
    }
}

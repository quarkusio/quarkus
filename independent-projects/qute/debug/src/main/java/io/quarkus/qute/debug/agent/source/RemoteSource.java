package io.quarkus.qute.debug.agent.source;

import java.net.URI;

import org.eclipse.lsp4j.debug.Source;

/**
 * Represents a remote Qute template source used in the debug agent.
 * <p>
 * This class extends the standard DAP {@link Source} to include additional
 * information relevant to Qute templates, such as:
 * </p>
 * <ul>
 * <li>The original {@link URI} of the template (if available)</li>
 * <li>The Qute {@code templateId} used to identify the template within the engine</li>
 * </ul>
 * <p>
 * Both fields are marked {@code transient} because they are not meant to be
 * serialized through the DAP protocol — only the standard {@link Source}
 * properties are transmitted to the client.
 * </p>
 */
public abstract class RemoteSource extends Source {

    /**
     * The URI of the template, if available.
     * <p>
     * May be {@code null} for in-memory or non-file-based templates.
     * </p>
     */
    private final transient URI uri;

    /**
     * The Qute template identifier, used internally by the Qute engine.
     */
    private final transient String templateId;

    /**
     * Creates a new remote source associated with the given template.
     *
     * @param uri the URI of the template source, or {@code null} if not applicable
     * @param templateId the template ID used by the Qute engine (never {@code null})
     */
    public RemoteSource(URI uri, String templateId) {
        this.uri = uri;
        this.templateId = templateId;

        // Initialize the DAP "name" field for display purposes in the client.
        // If the URI is known, extract the filename; otherwise, use the templateId.
        super.setName(uri != null ? computeName(uri) : templateId);
    }

    /**
     * Computes a human-readable name for this source, based on its URI.
     * <p>
     * The default implementation extracts the last segment (file name) from the URI.
     * Subclasses may override this method to provide a more descriptive name.
     * </p>
     *
     * @param uri the source URI
     * @return a display name for the source
     */
    protected String computeName(URI uri) {
        return getFileNameFallback(uri.toString());
    }

    /**
     * Returns the URI of this template source.
     *
     * @return the URI, or {@code null} if not applicable
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns the Qute template identifier.
     *
     * @return the template ID (never {@code null})
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * Utility method that extracts the file name from a URI or path string.
     * <p>
     * This method serves as a fallback when the standard {@link URI#getPath()} is
     * unavailable or when the input is not a valid URI.
     * </p>
     *
     * @param s a URI or path string
     * @return the last segment (file name), or the original string if no separator is found
     */
    private static String getFileNameFallback(String s) {
        int idx = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        if (idx != -1 && idx < s.length() - 1) {
            return s.substring(idx + 1);
        }
        return s; // Return the full string if no separator found
    }
}

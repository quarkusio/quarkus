package io.quarkus.qute.debug.agent.source;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.lsp4j.debug.SourceResponse;

/**
 * Registry responsible for managing {@link SourceResponse} objects used by the
 * Debug Adapter Protocol (DAP) to serve source content for Qute templates.
 * <p>
 * Each source reference corresponds to a unique integer ID associated with a
 * specific URI. This allows the DAP client to request the source content of a
 * template (e.g. a file embedded inside a JAR) by reference rather than by path.
 * </p>
 *
 * <p>
 * <strong>Usage example:</strong>
 * </p>
 *
 * <pre>{@code
 * int ref = registry.registerSourceReference(uri);
 * SourceResponse src = registry.getSourceReference(ref);
 * }</pre>
 */
public class SourceReferenceRegistry {

    private static final String QUTE_MIME_TYPE = "text/x-qute";

    /**
     * Counter used to generate unique source reference IDs.
     */
    private static final AtomicInteger sourceReferenceIdCounter = new AtomicInteger();

    /**
     * Stores all registered {@link SourceResponse} instances, indexed by their
     * unique source reference ID.
     */
    private final Map<Integer, SourceResponse> sourceReferences = new ConcurrentHashMap<>();

    /**
     * Returns the {@link SourceResponse} associated with the given source reference ID.
     *
     * @param sourceReference the ID assigned to the source reference
     * @return the corresponding {@link SourceResponse}, or {@code null} if not found
     */
    public SourceResponse getSourceReference(int sourceReference) {
        return sourceReferences.get(sourceReference);
    }

    /**
     * Registers a new {@link SourceResponse} for the given URI and assigns it
     * a unique source reference ID.
     * <p>
     * This method reads the template content directly from the JAR or file
     * specified by the URI, stores it in memory, and returns the assigned ID.
     * </p>
     *
     * @param uri the URI of the template (typically a {@code jar:file:...!/...} URI)
     * @return a unique source reference ID
     */
    public int registerSourceReference(URI uri) {
        int sourceReference = sourceReferenceIdCounter.incrementAndGet();
        String content = readFromJarUri(uri);

        SourceResponse response = new SourceResponse();
        response.setContent(content);
        response.setMimeType(QUTE_MIME_TYPE);

        sourceReferences.put(sourceReference, response);
        return sourceReference;
    }

    /**
     * Reads the content of the resource identified by the given URI.
     * <p>
     * This method is primarily intended for reading files stored inside JARs
     * (using {@code jar:file:...!/...} URIs), but also works for standard file URIs.
     * </p>
     *
     * @param uri the resource URI
     * @return the content of the resource as a UTF-8 string, or an empty string if an error occurs
     */
    public static String readFromJarUri(URI uri) {
        if (uri == null) {
            return "";
        }

        try {
            URL url = uri.toURL();
            try (InputStream in = url.openStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Clears all registered source references.
     * <p>
     * Typically called when restarting or detaching a debugging session.
     * </p>
     */
    public void reset() {
        sourceReferences.clear();
    }
}

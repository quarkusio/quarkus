package io.quarkus.qute.debug.agent.source;

import java.net.URI;

/**
 * A specialized {@link RemoteSource} representing a Qute template located
 * inside a JAR file.
 * <p>
 * This implementation is used when a template originates from a dependency JAR
 * rather than from the local file system. It ensures the source is properly
 * identified in the DAP client using a unique {@code sourceReference}.
 * </p>
 *
 * <p>
 * <b>Example URI:</b>
 * </p>
 *
 * <pre>
 * jar:file:///C:/Users/.../quarkus-renarde-3.1.2.jar!/templates/tags/ifError.html
 * </pre>
 *
 * <p>
 * The displayed name for this source will be:
 * </p>
 *
 * <pre>
 * quarkus-renarde-3.1.2.jar!/templates/tags/ifError.html
 * </pre>
 *
 * If extraction fails for any reason, the name falls back to the file name
 * (for example, {@code ifError.html}).
 */
public class JarSource extends RemoteSource {

    /**
     * Creates a new {@link JarSource} for a Qute template embedded in a JAR file.
     *
     * @param uri the full JAR URI of the template (e.g. {@code jar:file:///.../template.jar!/path/to/template.html})
     * @param templateId the Qute template identifier
     * @param sourceReferenceRegistry the registry responsible for assigning a unique DAP source reference
     */
    public JarSource(URI uri, String templateId, SourceReferenceRegistry sourceReferenceRegistry) {
        super(uri, templateId);
        super.setSourceReference(sourceReferenceRegistry.registerSourceReference(uri));
    }

    /**
     * Computes a human-readable name for this source, suitable for display
     * in the DAP client.
     * <p>
     * For JAR-based templates, the name includes the JAR file name and
     * the internal resource path (e.g. {@code my-lib.jar!/templates/foo.html}).
     * If the URI is not a JAR path or the format cannot be parsed,
     * this method falls back to {@link RemoteSource#computeName(URI)}.
     * </p>
     *
     * @param uri the full JAR URI of the source
     * @return a descriptive name for the source
     */
    @Override
    protected String computeName(URI uri) {
        String s = uri.toString();

        // Only process JAR URIs (e.g. "jar:file:///...")
        if (!s.startsWith("jar:file:")) {
            return super.computeName(uri);
        }

        // Strip the "jar:file:/" prefix
        s = s.substring("jar:file:/".length());

        // Normalize redundant slashes (mostly for Windows)
        while (s.startsWith("/")) {
            s = s.substring(1);
        }

        // Locate the ".jar!" separator, which marks the JAR boundary
        int jarIndex = s.lastIndexOf(".jar!");
        if (jarIndex == -1) {
            // No ".jar!" found → fallback to default name
            return super.computeName(uri);
        }

        // Extract everything from the JAR filename onward
        int startIndex = s.lastIndexOf('/', jarIndex - 1);
        if (startIndex == -1) {
            return super.computeName(uri);
        }

        return s.substring(startIndex + 1);
    }
}

package io.quarkus.qute.debug.agent.breakpoints;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;

import io.quarkus.qute.debug.agent.source.RemoteSource;
import io.quarkus.qute.debug.agent.source.SourceTemplateRegistry;

/**
 * Registry for managing Qute template breakpoints during a debugging session.
 * <p>
 * This class maintains a thread-safe mapping between template URIs and their
 * associated {@link RemoteBreakpoint}s, allowing the debug agent to quickly
 * resolve breakpoints by URI, template ID, or line number.
 * </p>
 */
public class BreakpointsRegistry {

    /**
     * Map of breakpoints per template URI.
     * <p>
     * Each entry maps a URI (template source) to a map of line numbers and
     * their associated {@link RemoteBreakpoint}s.
     * </p>
     */
    private final Map<URI, Map<Integer, RemoteBreakpoint>> breakpoints;

    /**
     * Creates an empty breakpoint registry.
     */
    public BreakpointsRegistry() {
        this.breakpoints = new ConcurrentHashMap<>();
    }

    /**
     * Sets (or replaces) the list of breakpoints for the given source.
     * <p>
     * This method is typically called in response to the DAP
     * {@code setBreakpoints} request. Existing breakpoints for the given
     * source are cleared and replaced with the provided ones.
     * </p>
     *
     * @param sourceBreakpoints the array of source breakpoints received from the client
     * @param source the DAP {@link Source} that identifies the template
     * @return an array of verified {@link Breakpoint}s to return to the client
     */
    public Breakpoint[] setBreakpoints(SourceBreakpoint[] sourceBreakpoints, Source source) {
        URI uri = SourceTemplateRegistry.toUri(source);
        Map<Integer, RemoteBreakpoint> templateBreakpoints = uri != null
                ? this.breakpoints.computeIfAbsent(uri, k -> new ConcurrentHashMap<>())
                : null;

        if (templateBreakpoints != null) {
            templateBreakpoints.clear();
        }

        Breakpoint[] result = new Breakpoint[sourceBreakpoints.length];
        for (int i = 0; i < sourceBreakpoints.length; i++) {
            SourceBreakpoint sourceBreakpoint = sourceBreakpoints[i];
            int line = sourceBreakpoint.getLine();
            String condition = sourceBreakpoint.getCondition();

            RemoteBreakpoint breakpoint = new RemoteBreakpoint(source, line, condition);

            if (templateBreakpoints != null) {
                templateBreakpoints.put(line, breakpoint);
                breakpoint.setVerified(true);
            } else {
                breakpoint.setVerified(false);
            }

            result[i] = breakpoint;
        }
        return result;
    }

    /**
     * Returns all URIs currently associated with breakpoints.
     *
     * @return a set of source URIs that have at least one breakpoint
     */
    public Set<URI> getSourceUris() {
        return breakpoints.keySet();
    }

    /**
     * Resolves a {@link RemoteBreakpoint} for the given template and line number.
     * <p>
     * This method first attempts to find a breakpoint by URI. If no URI is available,
     * it falls back to resolving the template URI from its ID using the provided
     * {@link SourceTemplateRegistry}.
     * </p>
     *
     * @param sourceUri the URI of the template, or {@code null} if unknown
     * @param templateId the template ID (used as a fallback when {@code sourceUri} is null)
     * @param line the line number in the template
     * @param sourceTemplateRegistry the registry used to resolve template sources
     * @return the matching {@link RemoteBreakpoint}, or {@code null} if none exists
     */
    public RemoteBreakpoint resolveBreakpoint(URI sourceUri, String templateId, int line,
            SourceTemplateRegistry sourceTemplateRegistry) {
        Map<Integer, RemoteBreakpoint> templateBreakpoints = findTemplateBreakpoints(sourceUri, templateId,
                sourceTemplateRegistry);
        return templateBreakpoints != null ? templateBreakpoints.get(line) : null;
    }

    /**
     * Finds the map of breakpoints for a given template.
     * <p>
     * If {@code sourceUri} is not provided, this method attempts to resolve the URI
     * from the given {@code templateId}.
     * </p>
     *
     * @param sourceUri the URI of the template, or {@code null}
     * @param templateId the template ID (used as fallback)
     * @param sourceTemplateRegistry the registry used to resolve the URI from the ID
     * @return a map of line numbers to {@link RemoteBreakpoint}s, or {@code null} if none found
     */
    private Map<Integer, RemoteBreakpoint> findTemplateBreakpoints(URI sourceUri, String templateId,
            SourceTemplateRegistry sourceTemplateRegistry) {
        if (sourceUri != null) {
            return this.breakpoints.get(sourceUri);
        }
        RemoteSource source = sourceTemplateRegistry.getSource(templateId, null);
        URI uri = source != null ? source.getUri() : null;
        return uri != null ? this.breakpoints.get(uri) : null;
    }

    /**
     * Clears all registered breakpoints.
     * <p>
     * Typically called when a debug session is terminated or reset.
     * </p>
     */
    public void reset() {
        this.breakpoints.clear();
    }
}

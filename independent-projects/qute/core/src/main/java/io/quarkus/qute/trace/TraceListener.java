package io.quarkus.qute.trace;

/**
 * Listener interface for tracing the rendering process of Qute templates.
 * <p>
 * Implementations receive callbacks at key points during template rendering:
 * <ul>
 * <li>When a template rendering starts and ends.</li>
 * <li>Before and after resolving each template node (expressions, sections,
 * etc.).</li>
 * </ul>
 * <p>
 * This enables logging, profiling, or building interactive debugging tools.
 */
public interface TraceListener {

    /**
     * Called when the rendering of a template starts.
     *
     * @param event the template event containing information about the template
     *        instance and timing
     */
    default void onStartTemplate(TemplateEvent event) {

    }

    /**
     * Called before a template node is resolved.
     *
     * @param event the resolve event containing the context and node to be resolved
     */
    default void onBeforeResolve(ResolveEvent event) {

    }

    /**
     * Called after a template node has been resolved.
     *
     * @param event the resolve event containing the context, node, result and any
     *        error encountered
     */
    default void onAfterResolve(ResolveEvent event) {

    }

    /**
     * Called when the rendering of a template ends.
     *
     * @param event the template event containing information about the template
     *        instance and timing
     */
    default void onEndTemplate(TemplateEvent event) {

    }
}

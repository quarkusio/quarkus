package io.quarkus.qute.trace;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.ResultNode;
import io.quarkus.qute.TemplateNode;

/**
 * Represents an event fired during the resolution of a template node.
 * <p>
 * This event encapsulates information about the node currently being resolved,
 * the resolution context, and the engine managing the rendering process.
 * <p>
 * It also holds the result of the resolution as a {@link ResultNode} and any
 * {@link Throwable} error that may have occurred during resolution. These
 * fields can be set by the code handling the event.
 * <p>
 * Used by trace listeners to monitor or inspect the evaluation of template
 * nodes.
 */
public final class ResolveEvent extends BaseEvent {

    private final TemplateNode templateNode;
    private final ResolutionContext context;
    private volatile ResultNode resultNode;
    private volatile Throwable error;

    /**
     * Creates a new {@code ResolveEvent} for the given template node, resolution
     * context, and engine.
     *
     * @param templateNode the template node being resolved
     * @param context the current resolution context
     * @param engine the engine managing the rendering
     */
    public ResolveEvent(TemplateNode templateNode, ResolutionContext context, Engine engine) {
        super(engine);
        this.templateNode = templateNode;
        this.context = context;
    }

    /**
     * Returns the template node currently being resolved.
     *
     * @return the template node
     */
    public TemplateNode getTemplateNode() {
        return templateNode;
    }

    /**
     * Returns the current resolution context.
     *
     * @return the resolution context
     */
    public ResolutionContext getContext() {
        return context;
    }

    /**
     * Returns the result of resolving the template node.
     * <p>
     * May be {@code null} if the resolution has not yet been performed or if an
     * error occurred.
     *
     * @return the result node or {@code null}
     */
    public ResultNode getResultNode() {
        return resultNode;
    }

    /**
     * Returns the error thrown during resolution, if any.
     * <p>
     * May be {@code null} if no error occurred.
     *
     * @return the error or {@code null}
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Sets the result of resolving the template node along with any error.
     *
     * @param resultNode the resolved result, or {@code null} if resolution failed
     * @param error the error thrown during resolution, or {@code null} if none
     */
    public void resolve(ResultNode resultNode, Throwable error) {
        this.resultNode = resultNode;
        this.error = error;
        super.done();
    }
}

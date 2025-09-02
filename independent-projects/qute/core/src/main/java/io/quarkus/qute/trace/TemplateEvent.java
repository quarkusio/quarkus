package io.quarkus.qute.trace;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateInstance;

/**
 * Event representing a significant moment in the lifecycle of a template
 * rendering, such as the start or the end of rendering a
 * {@link TemplateInstance}.
 * <p>
 * This event provides access to the {@link TemplateInstance} being rendered and
 * the {@link Engine} that manages the rendering process.
 * <p>
 * Typically used by {@link TraceListener}s to monitor template rendering
 * progress.
 *
 * @param templateInstance the template instance involved in the event
 * @param engine the engine managing the template rendering
 */
public final class TemplateEvent extends BaseEvent {

    private final TemplateInstance templateInstance;

    public TemplateEvent(TemplateInstance templateInstance, Engine engine) {
        super(engine);
        this.templateInstance = templateInstance;
    }

    public TemplateInstance getTemplateInstance() {
        return templateInstance;
    }

}
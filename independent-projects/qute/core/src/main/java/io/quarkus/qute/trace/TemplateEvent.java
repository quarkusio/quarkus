package io.quarkus.qute.trace;

import io.quarkus.qute.TemplateInstance;

public class TemplateEvent {

    private final TemplateInstance templateInstance;

    public TemplateEvent(TemplateInstance templateInstance) {
        this.templateInstance = templateInstance;
    }

    public TemplateInstance getTemplateInstance() {
        return templateInstance;
    }
}

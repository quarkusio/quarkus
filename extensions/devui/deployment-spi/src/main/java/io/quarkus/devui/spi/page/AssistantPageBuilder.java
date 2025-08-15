package io.quarkus.devui.spi.page;

public class AssistantPageBuilder extends WebComponentPageBuilder {

    protected AssistantPageBuilder() {
        super();
        icon("font-awesome-solid:robot");
        color("var(--quarkus-assistant)");
        tooltip("This uses the Quarkus Assistant feature");
        metadata("isAssistantPage", "true");
    }
}
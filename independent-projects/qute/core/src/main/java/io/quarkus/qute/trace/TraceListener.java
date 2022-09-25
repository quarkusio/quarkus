package io.quarkus.qute.trace;

public interface TraceListener {

    void beforeResolve(ResolveEvent event);

    void afterResolve(ResolveEvent event);

    void startTemplate(TemplateEvent event);

    void endTemplate(TemplateEvent event);

}

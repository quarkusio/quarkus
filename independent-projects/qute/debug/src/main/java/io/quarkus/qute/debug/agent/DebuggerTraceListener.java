package io.quarkus.qute.debug.agent;

import io.quarkus.qute.trace.ResolveEvent;
import io.quarkus.qute.trace.TemplateEvent;
import io.quarkus.qute.trace.TraceListener;

public class DebuggerTraceListener implements TraceListener {

    private final DebuggeeAgent agent;

    public DebuggerTraceListener(DebuggeeAgent agent) {
        this.agent = agent;
    }

    @Override
    public void onBeforeResolve(ResolveEvent event) {
        agent.onTemplateNode(event);
    }

    @Override
    public void onStartTemplate(TemplateEvent event) {
        agent.onStartTemplate(event);
    }

    @Override
    public void onEndTemplate(TemplateEvent event) {
        agent.onEndTemplate(event);
    }

}

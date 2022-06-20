package io.quarkus.qute.trace;

import io.quarkus.qute.Engine;
import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.ResultNode;
import io.quarkus.qute.TemplateNode;

public class ResolveEvent {

    private final TemplateNode templateNode;

    private final ResolutionContext context;

    private final Engine engine;

    private ResultNode resultNode;

    public ResolveEvent(TemplateNode templateNode, ResolutionContext context, Engine engine) {
        this.templateNode = templateNode;
        this.context = context;
        this.engine = engine;
    }

    public TemplateNode getTemplateNode() {
        return templateNode;
    }

    public ResolutionContext getContext() {
        return context;
    }

    public Engine getEngine() {
        return engine;
    }

    public ResultNode getResultNode() {
        return resultNode;
    }

    public void setResultNode(ResultNode resultNode) {
        this.resultNode = resultNode;
    }
}

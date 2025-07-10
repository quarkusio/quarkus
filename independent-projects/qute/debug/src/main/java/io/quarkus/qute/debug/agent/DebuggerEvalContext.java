package io.quarkus.qute.debug.agent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Expression;
import io.quarkus.qute.ResolutionContext;

public class DebuggerEvalContext implements EvalContext {

    private final Object base;
    private final RemoteStackFrame stackFrame;

    public DebuggerEvalContext(Object base, RemoteStackFrame stackFrame) {
        this.base = base;
        this.stackFrame = stackFrame;
    }

    @Override
    public Object getBase() {
        return base;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public List<Expression> getParams() {
        return Collections.emptyList();
    }

    @Override
    public CompletionStage<Object> evaluate(String expression) {
        return stackFrame.evaluate(expression);
    }

    @Override
    public CompletionStage<Object> evaluate(Expression expression) {
        return null;
    }

    @Override
    public Object getAttribute(String key) {
        return null;
    }

    @Override
    public ResolutionContext resolutionContext() {
        return stackFrame.getEvent().getContext();
    }

}

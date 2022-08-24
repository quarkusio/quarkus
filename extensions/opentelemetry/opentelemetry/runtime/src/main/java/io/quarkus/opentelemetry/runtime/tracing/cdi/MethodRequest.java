package io.quarkus.opentelemetry.runtime.tracing.cdi;

import java.lang.reflect.Method;

import io.opentelemetry.context.Context;

final class MethodRequest {
    private final Method method;
    private final Object[] args;

    private final Context actualContext;

    public MethodRequest(final Method method, final Object[] args, final Context actualContext) {
        this.method = method;
        this.args = args;
        this.actualContext = actualContext;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public Context getActualContext() {
        return actualContext;
    }
}

package io.quarkus.opentelemetry.runtime.tracing.cdi;

import java.lang.reflect.Method;

final class MethodRequest {
    private final Method method;
    private final Object[] args;

    public MethodRequest(final Method method, final Object[] args) {
        this.method = method;
        this.args = args;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }
}

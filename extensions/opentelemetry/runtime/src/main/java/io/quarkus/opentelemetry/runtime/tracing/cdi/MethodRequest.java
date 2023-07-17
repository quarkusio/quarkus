package io.quarkus.opentelemetry.runtime.tracing.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

class MethodRequest {
    private final Method method;
    private final Object[] args;
    private final Set<Annotation> annotationBindings;

    public MethodRequest(final Method method, final Object[] args, Set<Annotation> annotationBindings) {
        this.method = method;
        this.args = args;
        this.annotationBindings = annotationBindings;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public Set<Annotation> getAnnotationBindings() {
        return annotationBindings;
    }
}

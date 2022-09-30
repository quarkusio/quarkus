package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import io.quarkus.arc.MethodMetadata;

public class InterceptedMethodMetadata {

    public final List<InterceptorInvocation> chain;
    public final Method method;
    public final MethodMetadata methodMetadata;
    public final Set<Annotation> bindings;

    public InterceptedMethodMetadata(List<InterceptorInvocation> chain, Method method, MethodMetadata methodMetadata,
            Set<Annotation> bindings) {
        this.chain = chain;
        this.method = method;
        this.methodMetadata = methodMetadata;
        this.bindings = bindings;
    }

}

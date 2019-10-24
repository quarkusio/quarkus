package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class SubclassMethodMetadata {

    public final List<InterceptorInvocation> chain;
    public final Method method;
    public final Set<Annotation> bindings;

    public SubclassMethodMetadata(List<InterceptorInvocation> chain, Method method, Set<Annotation> bindings) {
        this.chain = chain;
        this.method = method;
        this.bindings = bindings;
    }

}

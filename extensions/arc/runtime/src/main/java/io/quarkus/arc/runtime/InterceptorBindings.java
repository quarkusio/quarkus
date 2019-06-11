package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.interceptor.InvocationContext;

import io.quarkus.arc.InvocationContextImpl;

public class InterceptorBindings {

    public static Set<Annotation> getInterceptorBindings(InvocationContext invocationContext) {
        if (invocationContext instanceof InvocationContextImpl) {
            return ((InvocationContextImpl) invocationContext).getInterceptorBindings();
        }
        return null;
    }
}

package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.ArcInvocationContext;

public class InterceptorBindings {

    @SuppressWarnings("unchecked")
    public static Set<Annotation> getInterceptorBindings(InvocationContext invocationContext) {
        return (Set<Annotation>) invocationContext.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
    }
}

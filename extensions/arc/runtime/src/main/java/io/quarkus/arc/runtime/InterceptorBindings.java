package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.AbstractAnnotationLiteral;
import io.quarkus.arc.ArcInvocationContext;

public class InterceptorBindings {

    @SuppressWarnings("unchecked")
    public static Set<Annotation> getInterceptorBindings(InvocationContext invocationContext) {
        return (Set<Annotation>) invocationContext.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
    }

    /**
     * This method is just a convenience for getting a hold of {@link AbstractAnnotationLiteral}.
     * See the Javadoc of the class for an explanation of the reasons it might be used {@link Annotation}.
     */
    @SuppressWarnings("unchecked")
    public static Set<AbstractAnnotationLiteral> getInterceptorBindingLiterals(InvocationContext invocationContext) {
        return (Set<AbstractAnnotationLiteral>) invocationContext.getContextData()
                .get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
    }
}

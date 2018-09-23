package org.jboss.shamrock.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.interceptor.InvocationContext;

import org.jboss.protean.arc.InvocationContextImpl;
import org.jboss.shamrock.runtime.InterceptorBindingService;

public class InterceptorBindingProvider implements InterceptorBindingService.Provider {
    @Override
    public Set<Annotation> getInterceptorBindings(InvocationContext invocationContext) {
        if (invocationContext instanceof InvocationContextImpl) {
            return ((InvocationContextImpl) invocationContext).getInterceptorBindings();
        }
        return null;
    }
}

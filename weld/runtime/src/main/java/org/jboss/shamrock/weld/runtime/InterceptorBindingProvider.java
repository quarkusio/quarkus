package org.jboss.shamrock.weld.runtime;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.interceptor.InvocationContext;

import org.jboss.shamrock.runtime.InterceptorBindingService;
import org.jboss.weld.interceptor.WeldInvocationContext;

public class InterceptorBindingProvider implements InterceptorBindingService.Provider {
    @Override
    public Set<Annotation> getInterceptorBindings(InvocationContext invocationContext) {
        if (invocationContext instanceof WeldInvocationContext) {
            return ((WeldInvocationContext) invocationContext).getInterceptorBindings();
        }
        return null;
    }
}

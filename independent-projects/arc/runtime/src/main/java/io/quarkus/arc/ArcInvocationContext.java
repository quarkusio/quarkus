package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.interceptor.InvocationContext;

/**
 * Enhanced version of {@link InvocationContext}.
 */
public interface ArcInvocationContext extends InvocationContext {

    /**
     * This key can be used to obtain the interceptor bindings from the context data.
     */
    @Deprecated
    String KEY_INTERCEPTOR_BINDINGS = "io.quarkus.arc.interceptorBindings";

    @Override
    Set<Annotation> getInterceptorBindings();

    /**
     * @deprecated use {@link #getInterceptorBinding(Class)}
     */
    @Deprecated
    <T extends Annotation> T findIterceptorBinding(Class<T> annotationType);

    /**
     * @deprecated use {@link #getInterceptorBindings(Class)}
     */
    @Deprecated
    <T extends Annotation> List<T> findIterceptorBindings(Class<T> annotationType);

    /**
     * @deprecated use {@link #getInterceptorBinding(Class)}
     */
    @Deprecated
    static <T extends Annotation> T findIterceptorBinding(InvocationContext context, Class<T> annotationType) {
        if (context instanceof ArcInvocationContext) {
            return ((ArcInvocationContext) context).findIterceptorBinding(annotationType);
        }
        return null;
    }

    /**
     * @deprecated use {@link #getInterceptorBindings(Class)}
     */
    @Deprecated
    static <T extends Annotation> List<T> findIterceptorBindings(InvocationContext context, Class<T> annotationType) {
        if (context instanceof ArcInvocationContext) {
            return ((ArcInvocationContext) context).findIterceptorBindings(annotationType);
        }
        return Collections.emptyList();
    }

}

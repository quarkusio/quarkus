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
    String KEY_INTERCEPTOR_BINDINGS = "io.quarkus.arc.interceptorBindings";

    /**
     *
     * @return the interceptor bindings
     */
    Set<Annotation> getInterceptorBindings();

    /**
     *
     * @param annotationType
     * @return the first interceptor binding found, or {@code null}
     */
    <T extends Annotation> T getInterceptorBinding(Class<T> annotationType);

    /**
     *
     * @param annotationType
     * @return the list of interceptor bindings of the given annotation type
     */
    <T extends Annotation> List<T> getInterceptorBindings(Class<T> annotationType);

    /**
     *
     * @param context
     * @param annotationType
     * @return the first interceptor binding found, or {@code null}
     */
    static <T extends Annotation> T getInterceptorBinding(InvocationContext context, Class<T> annotationType) {
        if (context instanceof ArcInvocationContext) {
            return ((ArcInvocationContext) context).getInterceptorBinding(annotationType);
        }
        return null;
    }

    /**
     *
     * @param context
     * @param annotationType
     * @return the list of interceptor bindings of the given annotation type
     */
    static <T extends Annotation> List<T> getInterceptorBindings(InvocationContext context, Class<T> annotationType) {
        if (context instanceof ArcInvocationContext) {
            return ((ArcInvocationContext) context).getInterceptorBindings(annotationType);
        }
        return Collections.emptyList();
    }

}

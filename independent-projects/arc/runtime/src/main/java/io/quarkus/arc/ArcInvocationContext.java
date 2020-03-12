package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Set;
import javax.interceptor.InvocationContext;

/**
 *
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

}

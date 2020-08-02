package io.quarkus.arc.deployment.staticmethods;

import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.InterceptorInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.util.HashUtil;

/**
 * Represents an intercepted static method.
 */
public final class InterceptedStaticMethodBuildItem extends MultiBuildItem {

    private final MethodInfo method;
    private final List<InterceptorInfo> interceptors;
    private final Set<AnnotationInstance> bindings;
    private final String hash;

    InterceptedStaticMethodBuildItem(MethodInfo method, Set<AnnotationInstance> bindings, List<InterceptorInfo> interceptors) {
        this.method = method;
        this.interceptors = interceptors;
        this.bindings = bindings;
        this.hash = HashUtil.sha1(method.declaringClass().name().toString() + method.toString());
    }

    public ClassInfo getTarget() {
        return method.declaringClass();
    }

    public MethodInfo getMethod() {
        return method;
    }

    /**
     * 
     * @return the list of interceptors that should be applied
     */
    public List<InterceptorInfo> getInterceptors() {
        return interceptors;
    }

    /**
     * 
     * @return the set of interceptor bindings
     */
    public Set<AnnotationInstance> getBindings() {
        return bindings;
    }

    /**
     * 
     * @return the unique hash that could be used to indentify the method
     */
    public String getHash() {
        return hash;
    }

}

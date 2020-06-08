package io.quarkus.arc.deployment;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.arc.processor.InterceptorResolver;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds a reference to the interceptor resolver.
 */
public final class InterceptorResolverBuildItem extends SimpleBuildItem {

    private final InterceptorResolver resolver;

    private final Set<DotName> interceptorBindings;

    InterceptorResolverBuildItem(BeanDeployment beanDeployment) {
        this.resolver = beanDeployment.getInterceptorResolver();
        this.interceptorBindings = Collections.unmodifiableSet(
                beanDeployment.getInterceptorBindings().stream().map(ClassInfo::name).collect(Collectors.toSet()));
    }

    public InterceptorResolver get() {
        return resolver;
    }

    /**
     * 
     * @return the set of all known interceptor bindings
     */
    public Set<DotName> getInterceptorBindings() {
        return interceptorBindings;
    }

}

package io.quarkus.arc.processor;

import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.spi.DefinitionException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

final class Interceptors {

    private Interceptors() {
    }

    /**
     *
     * @param interceptorClass
     * @param beanDeployment
     * @return a new interceptor info
     */
    static InterceptorInfo createInterceptor(ClassInfo interceptorClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer, AnnotationStore store) {
        Set<AnnotationInstance> bindings = new HashSet<>();
        Integer priority = 0;
        for (AnnotationInstance annotation : store.getAnnotations(interceptorClass)) {
            if (beanDeployment.getInterceptorBinding(annotation.name()) != null) {
                bindings.add(annotation);
                // can also be a transitive binding
                Set<AnnotationInstance> transitiveInterceptorBindings = beanDeployment
                        .getTransitiveInterceptorBindings(annotation.name());
                if (transitiveInterceptorBindings != null) {
                    bindings.addAll(transitiveInterceptorBindings);
                }
            } else if (annotation.name().equals(DotNames.PRIORITY)) {
                priority = annotation.value().asInt();
            }
        }
        if (bindings.isEmpty()) {
            throw new DefinitionException("Interceptor has no bindings: " + interceptorClass);
        }
        return new InterceptorInfo(interceptorClass, beanDeployment, bindings,
                Injection.forBean(interceptorClass, null, beanDeployment, transformer), priority);
    }

}

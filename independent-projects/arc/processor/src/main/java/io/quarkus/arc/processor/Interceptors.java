package io.quarkus.arc.processor;

import jakarta.enterprise.inject.spi.DefinitionException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;

final class Interceptors {

    static final Logger LOGGER = Logger.getLogger(Interceptors.class);

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
        Integer priority = null;
        for (AnnotationInstance annotation : store.getAnnotations(interceptorClass)) {
            bindings.addAll(beanDeployment.extractInterceptorBindings(annotation));
            // can also be a transitive binding
            Set<AnnotationInstance> transitiveInterceptorBindings = beanDeployment
                    .getTransitiveInterceptorBindings(annotation.name());
            if (transitiveInterceptorBindings != null) {
                bindings.addAll(transitiveInterceptorBindings);
            }

            if (annotation.name().equals(DotNames.PRIORITY)) {
                priority = annotation.value().asInt();
            }
            if (priority == null && annotation.name().equals(DotNames.ARC_PRIORITY)) {
                priority = annotation.value().asInt();
            }
        }
        if (bindings.isEmpty()) {
            throw new DefinitionException("Interceptor has no bindings: " + interceptorClass);
        }
        if (priority == null) {
            LOGGER.info("The interceptor " + interceptorClass + " does not declare any @Priority. " +
                    "It will be assigned a default priority value of 0.");
            priority = 0;
        }
        return new InterceptorInfo(interceptorClass, beanDeployment,
                bindings.size() == 1 ? Collections.singleton(bindings.iterator().next())
                        : Collections.unmodifiableSet(bindings),
                Injection.forBean(interceptorClass, null, beanDeployment, transformer), priority);
    }

}

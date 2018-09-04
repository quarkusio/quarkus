package org.jboss.protean.arc.processor;

import java.util.HashSet;
import java.util.Set;

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
    static InterceptorInfo createInterceptor(ClassInfo interceptorClass, BeanDeployment beanDeployment) {
        Set<AnnotationInstance> bindings = new HashSet<>();
        Integer priority = 0;
        for (AnnotationInstance annotation : interceptorClass.classAnnotations()) {
            if (beanDeployment.getInterceptorBinding(annotation.name()) != null) {
                bindings.add(annotation);
            } else if (annotation.name().equals(DotNames.PRIORITY)) {
                priority = annotation.value().asInt();
            }
        }
        return new InterceptorInfo(interceptorClass, beanDeployment, bindings, Injection.forBean(interceptorClass, beanDeployment), priority);
    }

}

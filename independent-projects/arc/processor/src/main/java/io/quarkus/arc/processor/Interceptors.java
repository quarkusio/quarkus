package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
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
            InjectionPointModifier transformer) {
        Set<AnnotationInstance> bindings = new HashSet<>();
        Integer priority = null;
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(interceptorClass)) {
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

            // rudimentary, but good enough for now (should also look at inherited annotations and stereotypes)
            ScopeInfo scope = beanDeployment.getScope(annotation.name());
            if (scope != null && !BuiltinScope.DEPENDENT.is(scope)) {
                throw new DefinitionException("Interceptor declares scope other than @Dependent: " + interceptorClass);
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

        checkInterceptorFieldsAndMethods(interceptorClass, beanDeployment);

        return new InterceptorInfo(interceptorClass, beanDeployment,
                bindings.size() == 1 ? Collections.singleton(bindings.iterator().next())
                        : Collections.unmodifiableSet(bindings),
                Injection.forBean(interceptorClass, null, beanDeployment, transformer), priority);
    }

    private static void checkInterceptorFieldsAndMethods(ClassInfo interceptorClass, BeanDeployment beanDeployment) {
        ClassInfo aClass = interceptorClass;
        while (aClass != null) {
            for (MethodInfo method : aClass.methods()) {
                if (beanDeployment.hasAnnotation(method, DotNames.PRODUCES)) {
                    throw new DefinitionException("Interceptor declares a producer method: " + interceptorClass);
                }
                // the following 3 checks rely on the annotation store returning parameter annotations for methods
                if (beanDeployment.hasAnnotation(method, DotNames.DISPOSES)) {
                    throw new DefinitionException("Interceptor declares a disposer method: " + interceptorClass);
                }
                if (beanDeployment.hasAnnotation(method, DotNames.OBSERVES)) {
                    throw new DefinitionException("Interceptor declares an observer method: " + interceptorClass);
                }
                if (beanDeployment.hasAnnotation(method, DotNames.OBSERVES_ASYNC)) {
                    throw new DefinitionException("Interceptor declares an async observer method: " + interceptorClass);
                }
            }

            for (FieldInfo field : aClass.fields()) {
                if (beanDeployment.hasAnnotation(field, DotNames.PRODUCES)) {
                    throw new DefinitionException("Interceptor declares a producer field: " + interceptorClass);
                }
            }

            DotName superClass = aClass.superName();
            aClass = superClass != null && !superClass.equals(DotNames.OBJECT)
                    ? getClassByName(beanDeployment.getBeanArchiveIndex(), superClass)
                    : null;
        }
    }

}

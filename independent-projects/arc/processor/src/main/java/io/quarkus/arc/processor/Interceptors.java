package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
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
     * @return a new interceptor info, or (only in strict mode) {@code null} if the interceptor is disabled
     */
    static InterceptorInfo createInterceptor(ClassInfo interceptorClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        Integer priority = null;
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(interceptorClass)) {
            if (annotation.name().equals(DotNames.PRIORITY)) {
                priority = annotation.value().asInt();
            }
            // rudimentary, but good enough for now (should also look at inherited annotations and stereotypes)
            ScopeInfo scope = beanDeployment.getScope(annotation.name());
            if (scope != null && !BuiltinScope.DEPENDENT.is(scope)) {
                throw new DefinitionException("Interceptor declares scope other than @Dependent: " + interceptorClass);
            }
        }

        Set<AnnotationInstance> bindings = new HashSet<>();
        addBindings(beanDeployment, interceptorClass, bindings, false);

        if (bindings.isEmpty()) {
            throw new DefinitionException("Interceptor has no bindings: " + interceptorClass);
        }

        if (priority == null) {
            if (beanDeployment.strictCompatibility) {
                // interceptor without `@Priority` is disabled per the specification
                return null;
            }

            LOGGER.info("The interceptor " + interceptorClass + " does not declare any @Priority. " +
                    "It will be assigned a default priority value of 0.");
            priority = 0;
        }

        checkClassLevelInterceptorBindings(bindings, interceptorClass, beanDeployment);
        checkInterceptorFieldsAndMethods(interceptorClass, beanDeployment);

        return new InterceptorInfo(interceptorClass, beanDeployment,
                bindings.size() == 1 ? Collections.singleton(bindings.iterator().next())
                        : Collections.unmodifiableSet(bindings),
                Injection.forBean(interceptorClass, null, beanDeployment, transformer, Injection.BeanType.INTERCEPTOR),
                priority);
    }

    private static void addBindings(BeanDeployment beanDeployment, ClassInfo classInfo, Collection<AnnotationInstance> bindings,
            boolean onlyInherited) {
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(classInfo)) {
            ClassInfo annotationClass = getClassByName(beanDeployment.getBeanArchiveIndex(), annotation.name());
            if (onlyInherited && !beanDeployment.hasAnnotation(annotationClass, DotNames.INHERITED)) {
                continue;
            }

            bindings.addAll(beanDeployment.extractInterceptorBindings(annotation));
        }

        if (classInfo.superName() != null && !classInfo.superName().equals(DotNames.OBJECT)) {
            ClassInfo superClass = getClassByName(beanDeployment.getBeanArchiveIndex(), classInfo.superName());
            if (superClass != null) {
                addBindings(beanDeployment, superClass, bindings, true);
            }
        }
    }

    // similar logic already exists in InterceptorResolver, but it doesn't validate
    // when called, `bindings` always include transitive bindings
    static void checkClassLevelInterceptorBindings(Collection<AnnotationInstance> bindings, ClassInfo targetClass,
            BeanDeployment beanDeployment) {
        IndexView index = beanDeployment.getBeanArchiveIndex();

        Map<DotName, List<AnnotationValue>> seenBindings = new HashMap<>();
        for (AnnotationInstance binding : bindings) {
            DotName name = binding.name();
            if (beanDeployment.hasAnnotation(index.getClassByName(name), DotNames.REPEATABLE)) {
                // don't validate @Repeatable interceptor bindings, repeatability is their entire point
                continue;
            }

            List<AnnotationValue> seenValues = seenBindings.get(name);
            if (seenValues != null) {
                // interceptor binding of the same type already seen
                // all annotation members (except nonbinding) must have equal values
                ClassInfo declaration = beanDeployment.getInterceptorBinding(name);
                Set<String> nonBindingMembers = beanDeployment.getInterceptorNonbindingMembers(name);

                for (AnnotationValue value : seenValues) {
                    if (declaration.method(value.name()).hasDeclaredAnnotation(DotNames.NONBINDING)
                            || nonBindingMembers.contains(value.name())) {
                        continue;
                    }

                    if (!value.equals(binding.valueWithDefault(index, value.name()))) {
                        throw new DefinitionException("Multiple instances of non-repeatable interceptor binding annotation "
                                + name + " with different member values on class " + targetClass);
                    }
                }
            } else {
                // interceptor binding of that type not seen yet, just remember it
                seenBindings.put(name, binding.valuesWithDefaults(index));
            }
        }
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

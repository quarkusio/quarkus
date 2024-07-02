package io.quarkus.arc.processor;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

class BindingsDiscovery {
    private final BeanDeployment beanDeployment;
    private final ClassInfo bindingsSourceClass;

    BindingsDiscovery(BeanDeployment beanDeployment, ClassInfo bindingsSourceClass) {
        this.beanDeployment = Objects.requireNonNull(beanDeployment);
        this.bindingsSourceClass = bindingsSourceClass;
    }

    boolean hasAnnotation(MethodInfo method, DotName annotation) {
        if (bindingsSourceClass == null) {
            return beanDeployment.hasAnnotation(method, annotation);
        }

        MethodInfo corresponding = findCorrespondingMethod(method);
        return corresponding != null && beanDeployment.hasAnnotation(corresponding, annotation);
    }

    Collection<AnnotationInstance> getAnnotations(MethodInfo method) {
        if (bindingsSourceClass == null) {
            return beanDeployment.getAnnotations(method);
        }

        MethodInfo corresponding = findCorrespondingMethod(method);
        return corresponding != null ? beanDeployment.getAnnotations(corresponding) : Set.of();
    }

    private MethodInfo findCorrespondingMethod(MethodInfo method) {
        for (MethodInfo candidate : bindingsSourceClass.methods()) {
            if (method.name().equals(candidate.name())
                    && method.returnType().equals(candidate.returnType())
                    && method.parameterTypes().equals(candidate.parameterTypes())
                    && Modifier.isStatic(method.flags()) == Modifier.isStatic(candidate.flags())) {
                return candidate;
            }
        }

        // no inheritance for now

        return null;
    }
}

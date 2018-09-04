package org.jboss.protean.arc.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 *
 * @author Martin Kouba
 */
class InjectionPointInfo {

    static InjectionPointInfo fromField(FieldInfo field, BeanDeployment beanDeployment) {
        return new InjectionPointInfo(field.type(),
                field.annotations().stream().filter(a -> beanDeployment.getQualifier(a.name()) != null).collect(Collectors.toSet()));
    }

    static List<InjectionPointInfo> fromMethod(MethodInfo method, BeanDeployment beanDeployment) {
        return fromMethod(method, beanDeployment, null);
    }

    static List<InjectionPointInfo> fromMethod(MethodInfo method, BeanDeployment beanDeployment, Predicate<Set<AnnotationInstance>> skiPredicate) {
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        for (ListIterator<Type> iterator = method.parameters().listIterator(); iterator.hasNext();) {
            Type paramType = iterator.next();
            Set<AnnotationInstance> paramAnnotations = new HashSet<>();
            for (AnnotationInstance annotation : method.annotations()) {
                if (Kind.METHOD_PARAMETER.equals(annotation.target().kind())
                        && annotation.target().asMethodParameter().position() == iterator.previousIndex()) {
                    paramAnnotations.add(annotation);
                }
            }
            if (skiPredicate != null && skiPredicate.test(paramAnnotations)) {
                // Skip parameter, e.g. @Disposes
                continue;
            }
            Set<AnnotationInstance> paramQualifiers = new HashSet<>();
            for (AnnotationInstance paramAnnotation : paramAnnotations) {
                if (beanDeployment.getQualifier(paramAnnotation.name()) != null) {
                    paramQualifiers.add(paramAnnotation);
                }
            }
            injectionPoints.add(new InjectionPointInfo(paramType, paramQualifiers));
        }
        return injectionPoints;
    }

    final Type requiredType;

    final Set<AnnotationInstance> requiredQualifiers;

    final AtomicReference<BeanInfo> resolvedBean;

    public InjectionPointInfo(Type requiredType, Set<AnnotationInstance> requiredQualifiers) {
        this.requiredType = requiredType;
        this.requiredQualifiers = requiredQualifiers.isEmpty()
                ? Collections.singleton(AnnotationInstance.create(DotNames.DEFAULT, null, Collections.emptyList()))
                : requiredQualifiers;
        this.resolvedBean = new AtomicReference<BeanInfo>(null);
    }

    void resolve(BeanInfo bean) {
        resolvedBean.set(bean);
    }

    BeanInfo getResolvedBean() {
        return resolvedBean.get();
    }

    @Override
    public String toString() {
        return "InjectionPointInfo [requiredType=" + requiredType + ", requiredQualifiers=" + requiredQualifiers + "]";
    }

}
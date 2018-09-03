package org.jboss.protean.arc.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
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

    static InjectionPointInfo from(FieldInfo field, BeanDeployment beanDeployment) {
        return new InjectionPointInfo(field.type(),
                field.annotations().stream().filter(a -> beanDeployment.getQualifier(a.name()) != null).collect(Collectors.toSet()));
    }

    static InjectionPointInfo from(MethodInfo method, int position, BeanDeployment beanDeployment) {
        Set<AnnotationInstance> paramQualifiers = method.annotations().stream().filter(a -> Kind.METHOD_PARAMETER.equals(a.target().kind())
                && a.target().asMethodParameter().position() == position && beanDeployment.getQualifier(a.name()) != null).collect(Collectors.toSet());
        return new InjectionPointInfo(method.parameters().get(position), paramQualifiers);
    }

    static List<InjectionPointInfo> from(MethodInfo method, BeanDeployment beanDeployment) {
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        for (int i = 0; i < method.parameters().size(); i++) {
            injectionPoints.add(from(method, i, beanDeployment));
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
package io.quarkus.arc.deployment.devconsole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.CompletedApplicationClassPredicateBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InterceptorInfo;

public class DevBeanInfo implements Comparable<DevBeanInfo> {

    public static DevBeanInfo from(BeanInfo bean, CompletedApplicationClassPredicateBuildItem predicate) {
        Set<Name> qualifiers = new HashSet<>();
        for (AnnotationInstance qualifier : bean.getQualifiers()) {
            qualifiers.add(Name.from(qualifier));
        }
        Set<Name> types = new HashSet<>();
        for (Type beanType : bean.getTypes()) {
            types.add(Name.from(beanType));
        }
        Name scope = Name.from(bean.getScope().getDotName());
        Name providerType = Name.from(bean.getProviderType());

        List<String> interceptors;
        List<InterceptorInfo> boundInterceptors = bean.getBoundInterceptors();
        if (boundInterceptors.isEmpty()) {
            interceptors = List.of();
        } else {
            interceptors = new ArrayList<>();
            for (InterceptorInfo interceptor : boundInterceptors) {
                interceptors.add(interceptor.getIdentifier());
            }
        }

        if (bean.getTarget().isPresent()) {
            AnnotationTarget target = bean.getTarget().get();
            DevBeanKind kind;
            String memberName;
            boolean isApplicationBean;
            Name declaringClass;
            if (target.kind() == Kind.METHOD) {
                MethodInfo method = target.asMethod();
                memberName = method.name();
                kind = DevBeanKind.METHOD;
                isApplicationBean = predicate.test(bean.getDeclaringBean().getBeanClass());
                declaringClass = Name.from(bean.getDeclaringBean().getBeanClass());
            } else if (target.kind() == Kind.FIELD) {
                FieldInfo field = target.asField();
                memberName = field.name();
                kind = DevBeanKind.FIELD;
                isApplicationBean = predicate.test(bean.getDeclaringBean().getBeanClass());
                declaringClass = Name.from(bean.getDeclaringBean().getBeanClass());
            } else if (target.kind() == Kind.CLASS) {
                ClassInfo clazz = target.asClass();
                kind = DevBeanKind.CLASS;
                memberName = null;
                isApplicationBean = predicate.test(clazz.name());
                declaringClass = null;
            } else {
                throw new IllegalArgumentException("Invalid annotation target: " + target);
            }
            return new DevBeanInfo(kind, isApplicationBean, providerType, memberName, types, qualifiers, scope, declaringClass,
                    interceptors);
        } else {
            // Synthetic bean
            return new DevBeanInfo(DevBeanKind.SYNTHETIC, false, providerType, null, types, qualifiers, scope, null,
                    interceptors);
        }
    }

    public DevBeanInfo(DevBeanKind kind, boolean isApplicationBean, Name providerType, String memberName, Set<Name> types,
            Set<Name> qualifiers, Name scope, Name declaringClass, List<String> boundInterceptors) {
        this.kind = kind;
        this.isApplicationBean = isApplicationBean;
        this.providerType = providerType;
        this.memberName = memberName;
        this.types = types;
        this.qualifiers = qualifiers;
        this.scope = scope;
        this.declaringClass = declaringClass;
        this.interceptors = boundInterceptors;
    }

    private final DevBeanKind kind;
    private final boolean isApplicationBean;
    private final Name providerType;
    private final String memberName;
    private final Set<Name> types;
    private final Set<Name> qualifiers;
    private final Name scope;
    private final Name declaringClass;
    private final List<String> interceptors;

    public DevBeanKind getKind() {
        return kind;
    }

    public Name getScope() {
        return scope;
    }

    public Set<Name> getQualifiers() {
        return qualifiers;
    }

    public Set<Name> getNonDefaultQualifiers() {
        Set<Name> nonDefault = new HashSet<>();
        String atDefault = DotNames.DEFAULT.toString();
        String atAny = DotNames.ANY.toString();
        for (Name qualifier : qualifiers) {
            if (qualifier.toString().endsWith(atDefault) || qualifier.toString().endsWith(atAny)) {
                continue;
            }
            nonDefault.add(qualifier);
        }
        return nonDefault;
    }

    public Set<Name> getTypes() {
        return types;
    }

    public Name getProviderType() {
        return providerType;
    }

    public String getMemberName() {
        return memberName;
    }

    public boolean isApplicationBean() {
        return isApplicationBean;
    }

    public Name getDeclaringClass() {
        return declaringClass;
    }

    public List<String> getInterceptors() {
        return interceptors;
    }

    @Override
    public int compareTo(DevBeanInfo o) {
        // Application beans should go first
        if (isApplicationBean == o.isApplicationBean) {
            return providerType.compareTo(o.providerType);
        }
        return isApplicationBean ? -1 : 1;
    }
}

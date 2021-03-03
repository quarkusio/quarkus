package io.quarkus.arc.deployment.devconsole;

import java.util.HashSet;
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

public class DevBeanInfo implements Comparable<DevBeanInfo> {

    private final DevBeanKind kind;
    private final boolean isApplicationBean;
    private final Name providerType;
    private final String memberName;
    private final Set<Name> types;
    private final Set<Name> qualifiers;
    private final Name scope;
    private final Name declaringClass;

    public DevBeanInfo(BeanInfo bean, CompletedApplicationClassPredicateBuildItem predicate) {
        qualifiers = new HashSet<>();
        for (AnnotationInstance qualifier : bean.getQualifiers()) {
            qualifiers.add(Name.from(qualifier));
        }
        scope = Name.from(bean.getScope().getDotName());
        types = new HashSet<>();
        for (Type beanType : bean.getTypes()) {
            types.add(Name.from(beanType));
        }

        providerType = Name.from(bean.getProviderType());

        if (bean.getTarget().isPresent()) {
            AnnotationTarget target = bean.getTarget().get();
            if (target.kind() == Kind.METHOD) {
                MethodInfo method = target.asMethod();
                memberName = method.name();
                this.kind = DevBeanKind.METHOD;
                this.isApplicationBean = predicate.test(bean.getDeclaringBean().getBeanClass());
                this.declaringClass = Name.from(bean.getDeclaringBean().getBeanClass());
            } else if (target.kind() == Kind.FIELD) {
                FieldInfo field = target.asField();
                this.memberName = field.name();
                this.kind = DevBeanKind.FIELD;
                this.isApplicationBean = predicate.test(bean.getDeclaringBean().getBeanClass());
                this.declaringClass = Name.from(bean.getDeclaringBean().getBeanClass());
            } else if (target.kind() == Kind.CLASS) {
                ClassInfo clazz = target.asClass();
                this.kind = DevBeanKind.CLASS;
                this.memberName = null;
                this.isApplicationBean = predicate.test(clazz.name());
                this.declaringClass = null;
            } else {
                throw new IllegalArgumentException("Invalid annotation target: " + target);
            }
        } else {
            // Synthetic bean
            this.kind = DevBeanKind.SYNTHETIC;
            this.isApplicationBean = false;
            this.declaringClass = null;
            this.memberName = null;
        }
    }

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

    @Override
    public int compareTo(DevBeanInfo o) {
        // Application beans should go first
        if (isApplicationBean == o.isApplicationBean) {
            return providerType.compareTo(o.providerType);
        }
        return isApplicationBean ? -1 : 1;
    }
}

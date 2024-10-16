package io.quarkus.arc.deployment.devui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
            boolean isGenerated = false;
            Name declaringClass;
            if (target.kind() == Kind.METHOD) {
                MethodInfo method = target.asMethod();
                memberName = method.name();
                kind = DevBeanKind.METHOD;
                isApplicationBean = predicate.test(bean.getDeclaringBean().getBeanClass());
                declaringClass = Name.from(bean.getDeclaringBean().getBeanClass());
                isGenerated = bean.getDeclaringBean().getImplClazz().isSynthetic();
            } else if (target.kind() == Kind.FIELD) {
                FieldInfo field = target.asField();
                memberName = field.name();
                kind = DevBeanKind.FIELD;
                isApplicationBean = predicate.test(bean.getDeclaringBean().getBeanClass());
                declaringClass = Name.from(bean.getDeclaringBean().getBeanClass());
                isGenerated = bean.getDeclaringBean().getImplClazz().isSynthetic();
            } else if (target.kind() == Kind.CLASS) {
                ClassInfo clazz = target.asClass();
                kind = DevBeanKind.CLASS;
                memberName = null;
                isApplicationBean = predicate.test(clazz.name());
                isGenerated = clazz.isSynthetic();
                declaringClass = null;
            } else {
                throw new IllegalArgumentException("Invalid annotation target: " + target);
            }
            return new DevBeanInfo(bean.getIdentifier(), kind, isApplicationBean, providerType, memberName, types, qualifiers,
                    scope, declaringClass,
                    interceptors, isGenerated);
        } else {
            // Synthetic bean
            return new DevBeanInfo(bean.getIdentifier(), DevBeanKind.SYNTHETIC, false, providerType, null, types, qualifiers,
                    scope, null,
                    interceptors, bean.getImplClazz().isSynthetic());
        }
    }

    public DevBeanInfo(String id, DevBeanKind kind, boolean isApplicationBean, Name providerType, String memberName,
            Set<Name> types,
            Set<Name> qualifiers, Name scope, Name declaringClass, List<String> boundInterceptors,
            boolean isGenerated) {
        this.id = id;
        this.kind = kind;
        this.isApplicationBean = isApplicationBean;
        this.providerType = providerType;
        this.memberName = memberName;
        this.types = types;
        this.qualifiers = qualifiers;
        this.scope = scope;
        this.declaringClass = declaringClass;
        this.interceptors = boundInterceptors;
        this.isGenerated = isGenerated;
    }

    private final String id;
    private final DevBeanKind kind;
    private final boolean isApplicationBean;
    private final Name providerType;
    private final String memberName;
    private final Set<Name> types;
    private final Set<Name> qualifiers;
    private final Name scope;
    private final Name declaringClass;
    private final List<String> interceptors;
    private final boolean isGenerated;

    public String getId() {
        return id;
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

    public List<String> getInterceptors() {
        return interceptors;
    }

    public boolean isGenerated() {
        return isGenerated;
    }

    // only exists to make sure that the JSON objects already have the field
    // and don't have to change their shape later
    public boolean isInactive() {
        return false;
    }

    public String getDescription() {
        return description(false);
    }

    public String getSimpleDescription() {
        return description(true);
    }

    private String description(boolean simple) {
        String typeInfo = typeInfo(simple);
        switch (kind) {
            case FIELD:
                return typeInfo + "#" + memberName;
            case METHOD:
                return typeInfo + "#" + memberName + "()";
            case SYNTHETIC:
                return "Synthetic: " + typeInfo;
            default:
                return typeInfo;
        }
    }

    public String typeInfo(boolean simple) {
        String type;
        switch (kind) {
            case FIELD:
            case METHOD:
                type = declaringClass.toString();
                break;
            default:
                type = providerType.toString();
                break;
        }
        if (simple) {
            int idx = type.lastIndexOf(".");
            return idx != -1 && type.length() > 1 ? type.substring(idx + 1) : type;
        }
        return type;
    }

    @Override
    public int compareTo(DevBeanInfo o) {
        // application beans come first
        int result = Boolean.compare(o.isApplicationBean, isApplicationBean);
        if (result != 0) {
            return result;
        }
        // generated beans comes last
        result = Boolean.compare(isGenerated, o.isGenerated);
        if (result != 0) {
            return result;
        }
        // fallback to name comparison
        return providerType.compareTo(o.providerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DevBeanInfo other = (DevBeanInfo) obj;
        return Objects.equals(id, other.id);
    }

}

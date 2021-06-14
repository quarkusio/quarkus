package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Annotations.contains;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;

/**
 * Represents an injection point.
 *
 * @author Martin Kouba
 */
public class InjectionPointInfo {

    static InjectionPointInfo fromField(FieldInfo field, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        Collection<AnnotationInstance> annotations = beanDeployment.getAnnotations(field);
        for (AnnotationInstance annotation : annotations) {
            beanDeployment.extractQualifiers(annotation).forEach(qualifiers::add);
        }
        Type type = resolveType(field.type(), beanClass, field.declaringClass(), beanDeployment);
        return new InjectionPointInfo(type,
                transformer.applyTransformers(type, field, qualifiers), field, -1,
                contains(annotations, DotNames.TRANSIENT_REFERENCE), contains(annotations, DotNames.DELEGATE));
    }

    static InjectionPointInfo fromResourceField(FieldInfo field, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        Type type = resolveType(field.type(), beanClass, field.declaringClass(), beanDeployment);
        return new InjectionPointInfo(type,
                transformer.applyTransformers(type, field, new HashSet<>(field.annotations())),
                InjectionPointKind.RESOURCE, field, -1, false, false);
    }

    static List<InjectionPointInfo> fromMethod(MethodInfo method, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        return fromMethod(method, beanClass, beanDeployment, null, transformer);
    }

    static List<InjectionPointInfo> fromMethod(MethodInfo method, ClassInfo beanClass, BeanDeployment beanDeployment,
            Predicate<Set<AnnotationInstance>> skipPredicate, InjectionPointModifier transformer) {
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        for (ListIterator<Type> iterator = method.parameters().listIterator(); iterator.hasNext();) {
            Type paramType = iterator.next();
            int position = iterator.previousIndex();
            Set<AnnotationInstance> paramAnnotations = Annotations.getParameterAnnotations(beanDeployment, method, position);
            if (skipPredicate != null && skipPredicate.test(paramAnnotations)) {
                // Skip parameter, e.g. @Disposes
                continue;
            }
            Set<AnnotationInstance> paramQualifiers = new HashSet<>();
            for (AnnotationInstance paramAnnotation : paramAnnotations) {
                beanDeployment.extractQualifiers(paramAnnotation).forEach(paramQualifiers::add);
            }
            Type type = resolveType(paramType, beanClass, method.declaringClass(), beanDeployment);
            injectionPoints.add(new InjectionPointInfo(type,
                    transformer.applyTransformers(type, method, paramQualifiers),
                    method, position, contains(paramAnnotations, DotNames.TRANSIENT_REFERENCE),
                    contains(paramAnnotations, DotNames.DELEGATE)));
        }
        return injectionPoints;
    }

    private final TypeAndQualifiers typeAndQualifiers;
    private final AtomicReference<BeanInfo> resolvedBean;
    private final InjectionPointKind kind;
    private final boolean hasDefaultedQualifier;
    private final AnnotationTarget target;
    private final int position;
    private final boolean isTransientReference;
    private final boolean isDelegate;

    InjectionPointInfo(Type requiredType, Set<AnnotationInstance> requiredQualifiers, AnnotationTarget target, int position,
            boolean isTransientReference, boolean isDelegate) {
        this(requiredType, requiredQualifiers, InjectionPointKind.CDI, target, position, isTransientReference, isDelegate);
    }

    InjectionPointInfo(Type requiredType, Set<AnnotationInstance> requiredQualifiers, InjectionPointKind kind,
            AnnotationTarget target, int position, boolean isTransientReference, boolean isDelegate) {
        this.typeAndQualifiers = new TypeAndQualifiers(requiredType,
                requiredQualifiers.isEmpty()
                        ? Collections.singleton(AnnotationInstance.create(DotNames.DEFAULT, null, Collections.emptyList()))
                        : requiredQualifiers);
        this.resolvedBean = new AtomicReference<BeanInfo>(null);
        this.kind = kind;
        this.hasDefaultedQualifier = requiredQualifiers.isEmpty();
        this.target = target;
        this.position = position;
        this.isTransientReference = isTransientReference;
        this.isDelegate = isDelegate;
    }

    void resolve(BeanInfo bean) {
        resolvedBean.set(bean);
    }

    BeanInfo getResolvedBean() {
        return resolvedBean.get();
    }

    InjectionPointKind getKind() {
        return kind;
    }

    /**
     * Note that for programmatic lookup, the required type is the type parameter specified at the injection point. For example,
     * the required type for an injection point of type {@code Instance<org.acme.Foo>} is {@code org.acme.Foo}.
     * 
     * @return the required type of this injection point
     */
    public Type getRequiredType() {
        Type requiredType = typeAndQualifiers.type;
        if (isProgrammaticLookup() && requiredType.kind() == org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE) {
            requiredType = requiredType.asParameterizedType().arguments().get(0);
        }
        return requiredType;
    }

    /**
     * This method always returns the original type declared on the injection point, unlike {@link #getRequiredType()}.
     * 
     * @return the type specified at the injection point
     */
    public Type getType() {
        return typeAndQualifiers.type;
    }

    /**
     * @return <code>true</code> if this injection represents a dynamically obtained instance, <code>false</code> otherwise
     */
    public boolean isProgrammaticLookup() {
        DotName requiredTypeName = typeAndQualifiers.type.name();
        return DotNames.INSTANCE.equals(requiredTypeName) || DotNames.INJECTABLE_INSTANCE.equals(requiredTypeName)
                || DotNames.PROVIDER.equals(requiredTypeName);
    }

    public Set<AnnotationInstance> getRequiredQualifiers() {
        return typeAndQualifiers.qualifiers;
    }

    public AnnotationInstance getRequiredQualifier(DotName name) {
        for (AnnotationInstance qualifier : typeAndQualifiers.qualifiers) {
            if (qualifier.name().equals(name)) {
                return qualifier;
            }
        }
        return null;
    }

    public boolean hasDefaultedQualifier() {
        return hasDefaultedQualifier;
    }

    TypeAndQualifiers getTypeAndQualifiers() {
        return typeAndQualifiers;
    }

    /**
     * For injected params, this method returns the corresponding method and not the param itself.
     * 
     * @return the annotation target
     */
    public AnnotationTarget getTarget() {
        return target;
    }

    public boolean isField() {
        return target.kind() == Kind.FIELD;
    }

    public boolean isParam() {
        return target.kind() == Kind.METHOD;
    }

    /**
     * 
     * @return true if this injection point represents a method parameter annotated with {@code TransientReference} that
     *         resolves to a dependent bean
     */
    boolean isDependentTransientReference() {
        BeanInfo bean = getResolvedBean();
        return bean != null && isParam() && BuiltinScope.DEPENDENT.is(bean.getScope()) && isTransientReference;
    }

    public boolean isTransientReference() {
        return isTransientReference;
    }

    public boolean isDelegate() {
        return isDelegate;
    }

    /**
     * @return the parameter position or {@code -1} for a field injection point
     */
    public int getPosition() {
        return position;
    }

    public String getTargetInfo() {
        switch (target.kind()) {
            case FIELD:
                return target.asField().declaringClass().name() + "#" + target.asField().name();
            case METHOD:
                return target.asMethod().declaringClass().name() + "#" + target.asMethod().name() + "()";
            default:
                return target.toString();
        }
    }

    @Override
    public String toString() {
        return "InjectionPointInfo [requiredType=" + typeAndQualifiers.type + ", requiredQualifiers="
                + typeAndQualifiers.qualifiers + "]";
    }

    private static Type resolveType(Type type, ClassInfo beanClass, ClassInfo declaringClass, BeanDeployment beanDeployment) {
        if (type.kind() == org.jboss.jandex.Type.Kind.CLASS) {
            return type;
        }
        Map<ClassInfo, Map<TypeVariable, Type>> resolvedTypeVariables = Types.resolvedTypeVariables(beanClass, beanDeployment);
        return resolveType(type, declaringClass, beanDeployment, resolvedTypeVariables);
    }

    private static Type resolveType(Type type, ClassInfo beanClass, BeanDeployment beanDeployment,
            Map<ClassInfo, Map<TypeVariable, Type>> resolvedTypeVariables) {
        if (type.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE) {
            if (resolvedTypeVariables.containsKey(beanClass)) {
                return resolvedTypeVariables.get(beanClass).getOrDefault(type.asTypeVariable(), type);
            }
        } else if (type.kind() == org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = type.asParameterizedType();
            Type[] typeParams = new Type[parameterizedType.arguments().size()];
            for (int i = 0; i < typeParams.length; i++) {
                Type argument = parameterizedType.arguments().get(i);
                if (argument.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE
                        || argument.kind() == org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE) {
                    typeParams[i] = resolveType(argument, beanClass, beanDeployment, resolvedTypeVariables);
                } else {
                    typeParams[i] = argument;
                }
            }
            return ParameterizedType.create(parameterizedType.name(), typeParams, parameterizedType.owner());
        }
        return type;
    }

    enum InjectionPointKind {
        CDI,
        RESOURCE
    }

    static class TypeAndQualifiers {

        final Type type;

        final Set<AnnotationInstance> qualifiers;

        public TypeAndQualifiers(Type type, Set<AnnotationInstance> qualifiers) {
            this.type = type;
            this.qualifiers = qualifiers;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((qualifiers == null) ? 0 : qualifiers.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
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
            TypeAndQualifiers other = (TypeAndQualifiers) obj;
            if (qualifiers == null) {
                if (other.qualifiers != null) {
                    return false;
                }
            } else if (!qualifiers.equals(other.qualifiers)) {
                return false;
            }
            if (type == null) {
                if (other.type != null) {
                    return false;
                }
            } else if (!type.equals(other.type)) {
                return false;
            }
            return true;
        }

    }

}

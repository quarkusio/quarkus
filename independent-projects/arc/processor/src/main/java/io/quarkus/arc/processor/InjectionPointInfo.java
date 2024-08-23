package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Annotations.contains;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

/**
 * Represents an injection point.
 *
 * @author Martin Kouba
 */
public class InjectionPointInfo {

    private static boolean isNamedWithoutValue(AnnotationInstance annotation) {
        if (annotation.name().equals(DotNames.NAMED)) {
            AnnotationValue name = annotation.value();
            return name == null || name.asString().isEmpty();
        }
        return false;
    }

    static InjectionPointInfo fromField(FieldInfo field, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        Collection<AnnotationInstance> annotations = beanDeployment.getAnnotations(field);
        for (AnnotationInstance annotation : annotations) {
            for (AnnotationInstance annotationInstance : beanDeployment.extractQualifiers(annotation)) {
                // if the qualifier is `@Named` without value, replace it with `@Named(fieldName)
                if (isNamedWithoutValue(annotationInstance)) {
                    annotationInstance = AnnotationInstance.builder(annotationInstance.name())
                            .value(field.name())
                            .buildWithTarget(annotationInstance.target());
                }
                qualifiers.add(annotationInstance);
            }
        }
        Type type = resolveType(field.type(), beanClass, field.declaringClass(), beanDeployment);
        return new InjectionPointInfo(type,
                transformer.applyTransformers(type, field, qualifiers), field, null,
                contains(annotations, DotNames.TRANSIENT_REFERENCE), contains(annotations, DotNames.DELEGATE));
    }

    static InjectionPointInfo fromResourceField(FieldInfo field, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        Type type = resolveType(field.type(), beanClass, field.declaringClass(), beanDeployment);
        return new InjectionPointInfo(type,
                transformer.applyTransformers(type, field, new HashSet<>(Annotations.onlyRuntimeVisible(field.annotations()))),
                InjectionPointKind.RESOURCE, field, null, false, false);
    }

    static List<InjectionPointInfo> fromMethod(MethodInfo method, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        return fromMethod(method, beanClass, beanDeployment, null, transformer);
    }

    static List<InjectionPointInfo> fromMethod(MethodInfo method, ClassInfo beanClass, BeanDeployment beanDeployment,
            BiPredicate<Set<AnnotationInstance>, Integer> skipPredicate, InjectionPointModifier transformer) {
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        for (ListIterator<Type> iterator = method.parameterTypes().listIterator(); iterator.hasNext();) {
            Type paramType = iterator.next();
            int position = iterator.previousIndex();
            Set<AnnotationInstance> paramAnnotations = Annotations.getParameterAnnotations(beanDeployment, method, position);
            if (skipPredicate != null && skipPredicate.test(paramAnnotations, position)) {
                // Skip parameter, e.g. @Disposes
                continue;
            }
            Set<AnnotationInstance> paramQualifiers = new HashSet<>();
            for (AnnotationInstance paramAnnotation : paramAnnotations) {
                for (AnnotationInstance annotationInstance : beanDeployment.extractQualifiers(paramAnnotation)) {
                    if (isNamedWithoutValue(annotationInstance)) {
                        throw new DefinitionException("@Named without value may not be used on method parameter: " + method);
                    }
                    paramQualifiers.add(annotationInstance);
                }
            }
            Type type = resolveType(paramType, beanClass, method.declaringClass(), beanDeployment);
            injectionPoints.add(new InjectionPointInfo(type,
                    transformer.applyTransformers(type, method, method.parameters().get(position), paramQualifiers),
                    method, method.parameters().get(position), contains(paramAnnotations, DotNames.TRANSIENT_REFERENCE),
                    contains(paramAnnotations, DotNames.DELEGATE)));
        }
        return injectionPoints;
    }

    static InjectionPointInfo fromSyntheticInjectionPoint(TypeAndQualifiers typeAndQualifiers) {
        return new InjectionPointInfo(typeAndQualifiers, InjectionPointKind.CDI, null, null, false, false);
    }

    private final TypeAndQualifiers typeAndQualifiers;
    private final AtomicReference<BeanInfo> resolvedBean;
    private final AtomicReference<BeanInfo> targetBean;
    private final InjectionPointKind kind;
    private final boolean hasDefaultQualifier;
    private final AnnotationTarget target;
    // once we remove #getTarget(), 'target' field should contain method parameter AnnotationTarget for method parameter injection
    // can be null for field injection as well as synth injection point
    private final AnnotationTarget methodParameterTarget;
    private final int position;
    private final boolean isTransientReference;
    private final boolean isDelegate;

    InjectionPointInfo(Type requiredType, Set<AnnotationInstance> requiredQualifiers, AnnotationTarget target,
            AnnotationTarget methodParameterTarget,
            boolean isTransientReference, boolean isDelegate) {
        this(requiredType, requiredQualifiers, InjectionPointKind.CDI, target, methodParameterTarget, isTransientReference,
                isDelegate);
    }

    InjectionPointInfo(Type requiredType, Set<AnnotationInstance> requiredQualifiers, InjectionPointKind kind,
            AnnotationTarget target, AnnotationTarget methodParameterTarget, boolean isTransientReference, boolean isDelegate) {
        this(new TypeAndQualifiers(requiredType, requiredQualifiers.isEmpty()
                ? Collections.singleton(AnnotationInstance.create(DotNames.DEFAULT, null, Collections.emptyList()))
                : requiredQualifiers),
                kind, target, methodParameterTarget, isTransientReference, isDelegate);
    }

    InjectionPointInfo(TypeAndQualifiers typeAndQualifiers, InjectionPointKind kind,
            AnnotationTarget target, AnnotationTarget methodParameterTarget, boolean isTransientReference, boolean isDelegate) {
        this.typeAndQualifiers = typeAndQualifiers;
        this.resolvedBean = new AtomicReference<BeanInfo>(null);
        this.targetBean = new AtomicReference<BeanInfo>(null);
        this.kind = kind;
        this.hasDefaultQualifier = typeAndQualifiers.qualifiers.size() == 1
                && typeAndQualifiers.qualifiers.iterator().next().name().equals(DotNames.DEFAULT);
        this.target = target;
        this.methodParameterTarget = methodParameterTarget;
        this.position = (methodParameterTarget == null || !Kind.METHOD_PARAMETER.equals(methodParameterTarget.kind())) ? -1
                : methodParameterTarget.asMethodParameter().position();
        this.isTransientReference = isTransientReference;
        this.isDelegate = isDelegate;

        // validation - Event injection point can never be a raw type
        if (DotNames.EVENT.equals(typeAndQualifiers.type.name()) && typeAndQualifiers.type.kind() == Type.Kind.CLASS) {
            throw new DefinitionException(
                    "Event injection point can never be raw type - please specify the type parameter. Injection point: "
                            + target);
        }
    }

    void resolve(BeanInfo bean) {
        resolvedBean.set(bean);
    }

    public BeanInfo getResolvedBean() {
        return resolvedBean.get();
    }

    public Optional<BeanInfo> getTargetBean() {
        return Optional.ofNullable(targetBean.get());
    }

    public void setTargetBean(BeanInfo bean) {
        this.targetBean.set(bean);
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
        return hasDefaultQualifier;
    }

    TypeAndQualifiers getTypeAndQualifiers() {
        return typeAndQualifiers;
    }

    /**
     * This method is deprecated and will be removed at some point after Quarkus 3.15.
     * Use {@link #getAnnotationTarget()} instead.
     * Both methods behave equally except for method parameter injection points where {@code getAnnotationTarget()}
     * returns method parameter as {@link AnnotationTarget} instead of the whole method.
     * <p>
     * For injected params, this method returns the corresponding method and not the param itself.
     *
     * @return the annotation target or {@code null} in case of synthetic injection point
     */
    @Deprecated(forRemoval = true, since = "3.12")
    public AnnotationTarget getTarget() {
        return target;
    }

    /**
     * Unlike {@link #getTarget()}, for injected params, this method returns the method parameter itself.
     *
     * @return the annotation target or {@code null} in case of synthetic injection point
     */
    public AnnotationTarget getAnnotationTarget() {
        return methodParameterTarget == null ? target : methodParameterTarget;
    }

    public boolean isField() {
        return target != null && target.kind() == Kind.FIELD;
    }

    public boolean isParam() {
        return methodParameterTarget != null && methodParameterTarget.kind() == Kind.METHOD_PARAMETER;
    }

    public boolean isTransient() {
        return isField() && Modifier.isTransient(target.asField().flags());
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

    public boolean hasResolvedBean() {
        return resolvedBean.get() != null;
    }

    /**
     * @return the parameter position or {@code -1} for a field injection point or synthetic injection point
     */
    public int getPosition() {
        return position;
    }

    public String getTargetInfo() {
        if (target == null) {
            return "";
        }
        switch (target.kind()) {
            case FIELD:
                return target.asField().declaringClass().name() + "#" + target.asField().name();
            case METHOD:
                String param = target.asMethod().parameterName(position);
                if (param == null || param.isBlank()) {
                    param = "arg" + position;
                }
                String method = target.asMethod().name();
                if (method.equals(Methods.INIT)) {
                    method = " constructor";
                } else {
                    method = "#" + method + "()";
                }
                return "parameter '" + param + "' of " + target.asMethod().declaringClass().name() + method;
            case METHOD_PARAMETER:
                String name = methodParameterTarget.asMethodParameter().method().name();
                if (name.equals(Methods.INIT)) {
                    name = " constructor";
                } else {
                    name = "#" + name + "()";
                }
                return "parameter '" + methodParameterTarget.asMethodParameter().name() + "' of "
                        + methodParameterTarget.asMethodParameter().method().declaringClass().name() + name;
            default:
                return target.toString();
        }
    }

    /**
     * @return {@code true} if it represents a synthetic injection point, {@code false} otherwise
     */
    public boolean isSynthetic() {
        return target == null;
    }

    /**
     * If an injection point resolves to a dependent bean that (A) injects the InjectionPoint metadata or (2) is synthetic, then
     * we need to wrap the injectable reference provider.
     *
     * @return {@code true} if a wrapper is needed, {@code false} otherwise
     */
    boolean isCurrentInjectionPointWrapperNeeded() {
        BeanInfo bean = getResolvedBean();
        if (bean != null && BuiltinScope.DEPENDENT.is(bean.getScope())) {
            return bean.isSynthetic() || bean.requiresInjectionPointMetadata();
        }
        return false;
    }

    @Override
    public String toString() {
        return "InjectionPointInfo [requiredType=" + typeAndQualifiers.type + ", requiredQualifiers="
                + typeAndQualifiers.qualifiers + "]";
    }

    private static Type resolveType(Type type, ClassInfo beanClass, ClassInfo declaringClass, BeanDeployment beanDeployment) {
        if (type.kind() == Type.Kind.PRIMITIVE || type.kind() == Type.Kind.CLASS) {
            return type;
        }
        Map<ClassInfo, Map<String, Type>> resolvedTypeVariables = Types.resolvedTypeVariables(beanClass, beanDeployment);
        return resolveType(type, declaringClass, beanDeployment, resolvedTypeVariables);
    }

    private static Type resolveType(Type type, ClassInfo beanClass, BeanDeployment beanDeployment,
            Map<ClassInfo, Map<String, Type>> resolvedTypeVariables) {
        if (type.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE) {
            if (resolvedTypeVariables.containsKey(beanClass)) {
                return resolvedTypeVariables.get(beanClass).getOrDefault(type.asTypeVariable().identifier(), type);
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
        } else if (type.kind() == org.jboss.jandex.Type.Kind.ARRAY) {
            ArrayType arrayType = type.asArrayType();
            Type component = arrayType.constituent();
            if (component.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE
                    || component.kind() == org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE) {
                component = resolveType(component, beanClass, beanDeployment, resolvedTypeVariables);
            }
            return ArrayType.create(component, type.asArrayType().dimensions());
        }
        return type;
    }

    enum InjectionPointKind {
        CDI,
        RESOURCE
    }

    public static class TypeAndQualifiers {

        public final Type type;

        public final Set<AnnotationInstance> qualifiers;

        public TypeAndQualifiers(Type type, Set<AnnotationInstance> qualifiers) {
            this.type = type;
            this.qualifiers = qualifiers;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            // We cannot use AnnotationInstance#hashCode() as it includes the AnnotationTarget
            result = prime * result + annotationSetHashCode(qualifiers);
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
            } else if (!annotationSetEquals(qualifiers, other.qualifiers)) {
                // We cannot use AnnotationInstance#equals() as it requires the exact same AnnotationTarget instance
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

        private static boolean annotationSetEquals(Set<AnnotationInstance> s1, Set<AnnotationInstance> s2) {
            if (s1 == s2) {
                return true;
            }
            if (s1.size() != s2.size()) {
                return false;
            }
            for (AnnotationInstance a1 : s1) {
                for (AnnotationInstance a2 : s2) {
                    if (!annotationEquals(a1, a2)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static boolean annotationEquals(AnnotationInstance a1, AnnotationInstance a2) {
            if (a1 == a2) {
                return true;
            }
            return a1.name().equals(a2.name()) && a1.values().equals(a2.values());
        }

        private static int annotationSetHashCode(Set<AnnotationInstance> s) {
            int result = 1;
            for (AnnotationInstance a : s) {
                result = 31 * result + annotationHashCode(a);
            }
            return result;
        }

        private static int annotationHashCode(AnnotationInstance a) {
            int result = a.name().hashCode();
            result = 31 * result + a.values().hashCode();
            return result;
        }

    }

}

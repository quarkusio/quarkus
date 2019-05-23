/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.processor;

import java.util.ArrayList;
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
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(field)) {
            if (beanDeployment.getQualifier(annotation.name()) != null) {
                qualifiers.add(annotation);
            }
        }
        Type type = resolveType(field.type(), beanClass, field.declaringClass(), beanDeployment);
        return new InjectionPointInfo(type,
                transformer.applyTransformers(type, field, qualifiers), field, -1);
    }

    static InjectionPointInfo fromResourceField(FieldInfo field, ClassInfo beanClass, BeanDeployment beanDeployment,
            InjectionPointModifier transformer) {
        Type type = resolveType(field.type(), beanClass, field.declaringClass(), beanDeployment);
        return new InjectionPointInfo(type,
                transformer.applyTransformers(type, field, new HashSet<>(field.annotations())),
                InjectionPointKind.RESOURCE, field, -1);
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
                if (beanDeployment.getQualifier(paramAnnotation.name()) != null) {
                    paramQualifiers.add(paramAnnotation);
                }
            }
            Type type = resolveType(paramType, beanClass, method.declaringClass(), beanDeployment);
            injectionPoints.add(new InjectionPointInfo(type,
                    transformer.applyTransformers(type, method, paramQualifiers),
                    method, position));
        }
        return injectionPoints;
    }

    private final TypeAndQualifiers typeAndQualifiers;

    private final AtomicReference<BeanInfo> resolvedBean;

    private final InjectionPointKind kind;

    private final boolean hasDefaultedQualifier;

    private final AnnotationTarget target;

    private final int position;

    InjectionPointInfo(Type requiredType, Set<AnnotationInstance> requiredQualifiers, AnnotationTarget target, int position) {
        this(requiredType, requiredQualifiers, InjectionPointKind.CDI, target, position);
    }

    InjectionPointInfo(Type requiredType, Set<AnnotationInstance> requiredQualifiers, InjectionPointKind kind,
            AnnotationTarget target, int position) {
        this.typeAndQualifiers = new TypeAndQualifiers(requiredType,
                requiredQualifiers.isEmpty()
                        ? Collections.singleton(AnnotationInstance.create(DotNames.DEFAULT, null, Collections.emptyList()))
                        : requiredQualifiers);
        this.resolvedBean = new AtomicReference<BeanInfo>(null);
        this.kind = kind;
        this.hasDefaultedQualifier = requiredQualifiers.isEmpty();
        this.target = target;
        this.position = position;
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

    public Type getRequiredType() {
        return typeAndQualifiers.type;
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

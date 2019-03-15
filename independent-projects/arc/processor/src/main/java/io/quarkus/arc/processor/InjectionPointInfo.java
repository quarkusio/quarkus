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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * Represents an injection point.
 *
 * @author Martin Kouba
 */
public class InjectionPointInfo {

    static InjectionPointInfo fromField(FieldInfo field, BeanDeployment beanDeployment) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(field)) {
            if (beanDeployment.getQualifier(annotation.name()) != null) {
                qualifiers.add(annotation);
            }
        }
        return new InjectionPointInfo(field.type(), qualifiers.isEmpty() ? Collections.emptySet() : qualifiers, field, -1);
    }

    static InjectionPointInfo fromResourceField(FieldInfo field, BeanDeployment beanDeployment) {
        return new InjectionPointInfo(field.type(), new HashSet<>(field.annotations()), InjectionPointKind.RESOURCE, field, -1);
    }

    static List<InjectionPointInfo> fromMethod(MethodInfo method, BeanDeployment beanDeployment) {
        return fromMethod(method, beanDeployment, null);
    }

    static List<InjectionPointInfo> fromMethod(MethodInfo method, BeanDeployment beanDeployment,
            Predicate<Set<AnnotationInstance>> skipPredicate) {
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        for (ListIterator<Type> iterator = method.parameters().listIterator(); iterator.hasNext();) {
            Type paramType = iterator.next();
            int position = iterator.previousIndex();
            Set<AnnotationInstance> paramAnnotations = getParameterAnnotations(beanDeployment, method, position);
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
            injectionPoints.add(new InjectionPointInfo(paramType, paramQualifiers, method, position));
        }
        return injectionPoints;
    }

    static Set<AnnotationInstance> getParameterAnnotations(BeanDeployment beanDeployment, MethodInfo method, int position) {
        Set<AnnotationInstance> annotations = new HashSet<>();
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(method)) {
            if (Kind.METHOD_PARAMETER.equals(annotation.target().kind())
                    && annotation.target().asMethodParameter().position() == position) {
                annotations.add(annotation);
            }
        }
        return annotations;
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

    enum InjectionPointKind {
        CDI, RESOURCE
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

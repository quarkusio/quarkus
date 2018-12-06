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

package org.jboss.protean.arc.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
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
        for (AnnotationInstance annotation : field.annotations()) {
            if (beanDeployment.getQualifier(annotation.name()) != null) {
                qualifiers.add(annotation);
            }
        }
        return new InjectionPointInfo(field.type(), qualifiers.isEmpty() ? Collections.emptySet() : qualifiers);
    }

    static InjectionPointInfo fromResourceField(FieldInfo field, BeanDeployment beanDeployment) {
        return new InjectionPointInfo(field.type(), new HashSet<>(field.annotations()), Kind.RESOURCE);
    }

    static List<InjectionPointInfo> fromMethod(MethodInfo method, BeanDeployment beanDeployment) {
        return fromMethod(method, beanDeployment, null);
    }

    static List<InjectionPointInfo> fromMethod(MethodInfo method, BeanDeployment beanDeployment, Predicate<Set<AnnotationInstance>> skipPredicate) {
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        for (ListIterator<Type> iterator = method.parameters().listIterator(); iterator.hasNext();) {
            Type paramType = iterator.next();
            Set<AnnotationInstance> paramAnnotations = new HashSet<>();
            for (AnnotationInstance annotation : method.annotations()) {
                if (org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER.equals(annotation.target().kind())
                        && annotation.target().asMethodParameter().position() == iterator.previousIndex()) {
                    paramAnnotations.add(annotation);
                }
            }
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
            injectionPoints.add(new InjectionPointInfo(paramType, paramQualifiers));
        }
        return injectionPoints;
    }

    private final TypeAndQualifiers typeAndQualifiers;

    private final AtomicReference<BeanInfo> resolvedBean;

    private final Kind kind;
    
    private final boolean hasDefaultedQualifier;

    InjectionPointInfo(Type requiredType, Set<AnnotationInstance> requiredQualifiers) {
        this(requiredType, requiredQualifiers, Kind.CDI);
    }

    InjectionPointInfo(Type requiredType, Set<AnnotationInstance> requiredQualifiers, Kind kind) {
        this.typeAndQualifiers = new TypeAndQualifiers(requiredType,
                requiredQualifiers.isEmpty() ? Collections.singleton(AnnotationInstance.create(DotNames.DEFAULT, null, Collections.emptyList()))
                        : requiredQualifiers);
        this.resolvedBean = new AtomicReference<BeanInfo>(null);
        this.kind = kind;
        this.hasDefaultedQualifier = requiredQualifiers.isEmpty();
    }

    void resolve(BeanInfo bean) {
        resolvedBean.set(bean);
    }

    BeanInfo getResolvedBean() {
        return resolvedBean.get();
    }

    Kind getKind() {
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

    @Override
    public String toString() {
        return "InjectionPointInfo [requiredType=" + typeAndQualifiers.type + ", requiredQualifiers=" + typeAndQualifiers.qualifiers + "]";
    }

    enum Kind {
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
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

package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 *
 * @author Martin Kouba
 */
public class CurrentInjectionPointProvider<T> implements InjectableReferenceProvider<T> {

    static final InjectionPoint EMPTY = new InjectionPointImpl(Object.class, Object.class, Collections.emptySet(), null, null,
            null, -1);

    private final InjectableReferenceProvider<T> delegate;

    private final InjectionPoint injectionPoint;

    public CurrentInjectionPointProvider(InjectableBean<?> bean, InjectableReferenceProvider<T> delegate, Type requiredType,
            Set<Annotation> qualifiers, Set<Annotation> annotations, Member javaMember, int position) {
        this.delegate = delegate;
        this.injectionPoint = new InjectionPointImpl(requiredType, requiredType, qualifiers, bean, annotations, javaMember,
                position);
    }

    @Override
    public T get(CreationalContext<T> creationalContext) {
        InjectionPoint prev = InjectionPointProvider.CURRENT.get();
        InjectionPointProvider.CURRENT.set(injectionPoint);
        try {
            return delegate.get(creationalContext);
        } finally {
            if (prev != null) {
                InjectionPointProvider.CURRENT.set(prev);
            } else {
                InjectionPointProvider.CURRENT.remove();
            }
        }
    }

    static class InjectionPointImpl implements InjectionPoint {

        private final Type requiredType;
        private final Set<Annotation> qualifiers;
        private final InjectableBean<?> bean;
        private final Annotated annotated;
        private final Member member;

        InjectionPointImpl(Type injectionPointType, Type requiredType, Set<Annotation> qualifiers, InjectableBean<?> bean,
                Set<Annotation> annotations,
                Member javaMember, int position) {
            this.requiredType = requiredType;
            this.qualifiers = qualifiers;
            this.bean = bean;
            if (javaMember instanceof Executable) {
                this.annotated = new AnnotatedParameterImpl<>(injectionPointType, annotations, position,
                        (Executable) javaMember);
            } else {
                this.annotated = new AnnotatedFieldImpl<>(injectionPointType, annotations, (Field) javaMember);
            }
            this.member = javaMember;
        }

        @Override
        public Type getType() {
            return requiredType;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        @Override
        public Bean<?> getBean() {
            return bean;
        }

        @Override
        public Member getMember() {
            return member;
        }

        @Override
        public Annotated getAnnotated() {
            return annotated;
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }

    }

    static class AnnotatedFieldImpl<X> extends AnnotatedBase implements AnnotatedField<X> {

        private final Field field;

        AnnotatedFieldImpl(Type baseType, Set<Annotation> annotations, Field field) {
            super(baseType, annotations);
            this.field = field;
        }

        @Override
        public boolean isStatic() {
            return Modifier.isStatic(field.getModifiers());
        }

        @SuppressWarnings("unchecked")
        @Override
        public AnnotatedType<X> getDeclaringType() {
            return new AnnotatedTypeImpl<>((Class<X>) field.getDeclaringClass());
        }

        @Override
        public Field getJavaMember() {
            return field;
        }

    }

    static class AnnotatedParameterImpl<X> extends AnnotatedBase implements AnnotatedParameter<X> {

        private final int position;
        private final Executable executable;

        AnnotatedParameterImpl(Type baseType, Set<Annotation> annotations, int position, Executable executable) {
            super(baseType, annotations);
            this.position = position;
            this.executable = executable;
        }

        @Override
        public int getPosition() {
            return position;
        }

        @SuppressWarnings("unchecked")
        @Override
        public AnnotatedCallable<X> getDeclaringCallable() {
            if (executable instanceof Method) {
                return new AnnotatedMethodImpl<>((Method) executable);
            } else {
                return new AnnotatedConstructorImpl<X>((Constructor<X>) executable);
            }
        }

    }

    static abstract class AnnotatedBase implements Annotated {

        private final Type baseType;
        private final Set<Annotation> annotations;

        AnnotatedBase(Type baseType, Set<Annotation> annotations) {
            this.baseType = baseType;
            this.annotations = annotations;
        }

        @Override
        public Type getBaseType() {
            return baseType;
        }

        @Override
        public Set<Type> getTypeClosure() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
            if (annotations == null) {
                throw new UnsupportedOperationException();
            }
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(annotationType)) {
                    return (T) annotation;
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
            if (annotations == null) {
                throw new UnsupportedOperationException();
            }
            Set<T> found = new HashSet<>();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(annotationType)) {
                    found.add((T) annotation);
                }
            }
            return found;
        }

        @Override
        public Set<Annotation> getAnnotations() {
            if (annotations == null) {
                throw new UnsupportedOperationException();
            }
            return Collections.unmodifiableSet(annotations);
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return getAnnotation(annotationType) != null;
        }

    }

    static class AnnotatedTypeImpl<X> extends AnnotatedBase implements AnnotatedType<X> {

        private final Class<X> clazz;

        AnnotatedTypeImpl(Class<X> clazz) {
            super(clazz, null);
            this.clazz = clazz;
        }

        @Override
        public Class<X> getJavaClass() {
            return clazz;
        }

        @Override
        public Set<AnnotatedConstructor<X>> getConstructors() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<AnnotatedMethod<? super X>> getMethods() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<AnnotatedField<? super X>> getFields() {
            throw new UnsupportedOperationException();
        }

    }

    static class AnnotatedMethodImpl<X> extends AnnotatedBase implements AnnotatedMethod<X> {

        private final Method method;

        AnnotatedMethodImpl(Method method) {
            super(method.getGenericReturnType(), null);
            this.method = method;
        }

        @Override
        public List<AnnotatedParameter<X>> getParameters() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isStatic() {
            return Modifier.isStatic(method.getModifiers());
        }

        @SuppressWarnings("unchecked")
        @Override
        public AnnotatedType<X> getDeclaringType() {
            return new AnnotatedTypeImpl<>((Class<X>) method.getDeclaringClass());
        }

        @Override
        public Method getJavaMember() {
            return method;
        }

    }

    static class AnnotatedConstructorImpl<X> extends AnnotatedBase implements AnnotatedConstructor<X> {

        private final Constructor<X> constructor;

        public AnnotatedConstructorImpl(Constructor<X> constructor) {
            super(constructor.getDeclaringClass(), null);
            this.constructor = constructor;
        }

        @Override
        public List<AnnotatedParameter<X>> getParameters() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public AnnotatedType<X> getDeclaringType() {
            return new AnnotatedTypeImpl<>((Class<X>) constructor.getDeclaringClass());
        }

        @Override
        public Constructor<X> getJavaMember() {
            return constructor;
        }

    }

}

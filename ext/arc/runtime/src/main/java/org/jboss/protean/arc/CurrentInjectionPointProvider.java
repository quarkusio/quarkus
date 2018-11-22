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

package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 *
 * @author Martin Kouba
 */
public class CurrentInjectionPointProvider<T> implements InjectableReferenceProvider<T> {

    static final InjectionPoint EMPTY = new InjectionPointImpl(Object.class, Collections.emptySet());

    private final InjectableReferenceProvider<T> delegate;

    private final InjectionPoint injectionPoint;

    public CurrentInjectionPointProvider(InjectableReferenceProvider<T> delegate, Type requiredType, Set<Annotation> qualifiers) {
        this.delegate = delegate;
        this.injectionPoint = new InjectionPointImpl(requiredType, qualifiers);
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

    private static class InjectionPointImpl implements InjectionPoint {

        private final Type requiredType;

        private final Set<Annotation> qualifiers;

        InjectionPointImpl(Type requiredType, Set<Annotation> qualifiers) {
            this.requiredType = requiredType;
            this.qualifiers = qualifiers;
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
            return null;
        }

        @Override
        public Member getMember() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Annotated getAnnotated() {
            throw new UnsupportedOperationException();
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

}

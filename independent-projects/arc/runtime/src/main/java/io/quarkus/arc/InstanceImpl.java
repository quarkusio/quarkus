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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Provider;

/**
 *
 * @author Martin Kouba
 */
class InstanceImpl<T> implements Instance<T> {

    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[] {};

    private final Type type;

    private final Set<Annotation> qualifiers;

    private final CreationalContextImpl<?> creationalContext;

    private final Set<InjectableBean<?>> beans;

    InstanceImpl(Type type, Set<Annotation> qualifiers, CreationalContextImpl<?> creationalContext) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (Provider.class.isAssignableFrom(Types.getRawType(parameterizedType.getRawType()))) {
                this.type = parameterizedType.getActualTypeArguments()[0];
            } else {
                this.type = type;
            }
        } else {
            this.type = type;
        }
        this.qualifiers = qualifiers != null ? qualifiers : Collections.emptySet();
        this.creationalContext = creationalContext;

        if (this.qualifiers.isEmpty() && Object.class.equals(type)) {
            // Do not prefetch the beans for Instance<Object> with no qualifiers
            this.beans = null;
        } else {
            this.beans = resolve();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new InstanceIterator(beans());
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        Set<InjectableBean<?>> beans = beans();
        if (beans.isEmpty()) {
            throw new UnsatisfiedResolutionException();
        } else if (beans.size() > 1) {
            throw new AmbiguousResolutionException();
        }
        return getBeanInstance((InjectableBean<T>) beans.iterator().next());
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(newQualifiers, qualifiers);
        return new InstanceImpl<>(type, newQualifiers, creationalContext);
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(newQualifiers, qualifiers);
        return new InstanceImpl<>(subtype, newQualifiers, creationalContext);
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(newQualifiers, qualifiers);
        return new InstanceImpl<>(subtype.getType(), newQualifiers, creationalContext);
    }

    @Override
    public boolean isUnsatisfied() {
        return beans().isEmpty();
    }

    @Override
    public boolean isAmbiguous() {
        return beans().size() > 1;
    }

    @Override
    public void destroy(T instance) {
        if (instance instanceof ClientProxy) {
            throw new UnsupportedOperationException();
        }
        // Try to destroy a dependent instance
        creationalContext.destroyDependentInstance(instance);
    }

    void destroy() {
        creationalContext.release();
    }

    private T getBeanInstance(InjectableBean<T> bean) {
        CreationalContextImpl<T> ctx = creationalContext.child();
        // TODO current injection point?
        T instance = bean.get(ctx);
        return instance;
    }

    private Set<InjectableBean<?>> beans() {
        return beans != null ? beans : resolve();
    }

    private Set<InjectableBean<?>> resolve() {
        return ArcContainerImpl.instance().getResolvedBeans(type, qualifiers.toArray(EMPTY_ANNOTATION_ARRAY));
    }

    class InstanceIterator implements Iterator<T> {

        protected final Iterator<InjectableBean<?>> delegate;

        private InstanceIterator(Collection<InjectableBean<?>> beans) {
            this.delegate = beans.iterator();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next() {
            return getBeanInstance((InjectableBean<T>) delegate.next());
        }

    }

}

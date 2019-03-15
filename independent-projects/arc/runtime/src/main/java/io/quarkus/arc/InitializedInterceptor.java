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
import java.lang.reflect.Type;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.InvocationContext;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public class InitializedInterceptor<T> implements InjectableInterceptor<T> {

    public static <I> InitializedInterceptor<I> of(I interceptorInstance, InjectableInterceptor<I> delegate) {
        return new InitializedInterceptor<>(interceptorInstance, delegate);
    }

    private final T interceptorInstance;

    private final InjectableInterceptor<T> delegate;

    InitializedInterceptor(T interceptorInstance, InjectableInterceptor<T> delegate) {
        this.interceptorInstance = interceptorInstance;
        this.delegate = delegate;
    }

    @Override
    public String getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return delegate.getScope();
    }

    @Override
    public Set<Type> getTypes() {
        return delegate.getTypes();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return delegate.getQualifiers();
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        return delegate.create(creationalContext);
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        delegate.destroy(instance, creationalContext);
    }

    @Override
    public T get(CreationalContext<T> creationalContext) {
        return interceptorInstance;
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return delegate.getInterceptorBindings();
    }

    @Override
    public boolean intercepts(InterceptionType type) {
        return delegate.intercepts(type);
    }

    @Override
    public Object intercept(InterceptionType type, T instance, InvocationContext ctx) throws Exception {
        return delegate.intercept(type, instance, ctx);
    }

    @Override
    public int getPriority() {
        return delegate.getPriority();
    }

    @Override
    public Class<?> getBeanClass() {
        return delegate.getBeanClass();
    }

}

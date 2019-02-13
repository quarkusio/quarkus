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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
class InstanceHandleImpl<T> implements InstanceHandle<T> {

    @SuppressWarnings("unchecked")
    public static final <T> InstanceHandle<T> unavailable() {
        return (InstanceHandle<T>) UNAVAILABLE;
    }

    static final InstanceHandleImpl<Object> UNAVAILABLE = new InstanceHandleImpl<Object>(null, null, null, null);

    private final InjectableBean<T> bean;

    private final T instance;

    private final CreationalContext<T> creationalContext;

    private final CreationalContext<?> parentCreationalContext;
    
    private final AtomicBoolean destroyed;

    InstanceHandleImpl(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext) {
        this(bean, instance, creationalContext, null);
    }

    InstanceHandleImpl(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext, CreationalContext<?> parentCreationalContext) {
        this.bean = bean;
        this.instance = instance;
        this.creationalContext = creationalContext;
        this.parentCreationalContext = parentCreationalContext;
        this.destroyed = new AtomicBoolean(false);
    }

    @Override
    public T get() {
        if (destroyed.get()) {
            throw new IllegalStateException("Instance already destroyed");
        }
        return instance;
    }

    @Override
    public InjectableBean<T> getBean() {
        return bean;
    }

    @Override
    public void destroy() {
        if (instance != null && destroyed.compareAndSet(false, true)) {
            if (bean.getScope().equals(Dependent.class)) {
                destroyInternal();
            } else {
                Arc.container().getContext(bean.getScope()).destroy(bean);
            }
        }
    }

    void destroyInternal() {
        if (parentCreationalContext != null) {
            parentCreationalContext.release();
        } else {
            bean.destroy(instance, creationalContext);
        }
    }

    static <T> InstanceHandleImpl<T> unwrap(InstanceHandle<T> handle) {
        if (handle instanceof InstanceHandleImpl) {
            return (InstanceHandleImpl<T>) handle;
        } else {
            throw new IllegalArgumentException("Failed to unwrap InstanceHandleImpl: " + handle);
        }
    }

}

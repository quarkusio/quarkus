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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

abstract class AbstractSharedContext implements InjectableContext {

    private final ComputingCache<Key<?>, InstanceHandleImpl<?>> instances;

    @SuppressWarnings("rawtypes")
    public AbstractSharedContext() {
        this.instances = new ComputingCache<>(key -> createInstanceHandle((InjectableBean) key.contextual, key.creationalContext));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        return (T) instances.getValue(new Key<>(contextual, creationalContext)).get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual) {
        InstanceHandleImpl<?> handle = instances.getValueIfPresent(new Key<>(contextual, null));
        return handle != null ? (T) handle.get() : null;
    }

    @Override
    public Collection<InstanceHandle<?>> getAll() {
        List<InstanceHandle<?>> all = new ArrayList<>();
        instances.forEachValue(all::add);
        return all;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        InstanceHandleImpl<?> handle = instances.remove(new Key<>(contextual, null));
        if (handle != null) {
            handle.destroyInternal();
        }
    }

    @Override
    public synchronized void destroy() {
        instances.forEachExistingValue(InstanceHandleImpl::destroyInternal);
        instances.clear();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static InstanceHandleImpl createInstanceHandle(InjectableBean bean, CreationalContext ctx) {
        return new InstanceHandleImpl(bean, bean.create(ctx), ctx);
    }

    private static class Key<T> {

        private Contextual<T> contextual;

        private CreationalContext<T> creationalContext;

        public Key(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            this.contextual = contextual;
            this.creationalContext = creationalContext;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((contextual == null) ? 0 : contextual.hashCode());
            return result;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            Key other = (Key) obj;
            if (contextual == null) {
                if (other.contextual != null) {
                    return false;
                }
            } else if (!contextual.equals(other.contextual)) {
                return false;
            }
            return true;
        }

    }

}

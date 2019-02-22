/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator.config.reader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class MappedPropertiesHandler<T> implements PropertiesHandler<T> {

    public interface PropertySetter<T> {
        void setProperty(T t, String value) throws PropertiesConfigReaderException;
    }

    public interface NestedSetter<P, C> {
        void setNested(P p, C c) throws PropertiesConfigReaderException;
    }

    public interface RootSetter<C> {
        void setRoot(C c) throws PropertiesConfigReaderException;
    }

    static class NestedHandlerWithSetter<P, C> implements PropertiesHandler<C> {
        private final PropertiesHandler<C> delegate;
        private final NestedSetter<P, C> nestedSetter;
        private final RootSetter<C> nestedNamedSetter;

        public NestedHandlerWithSetter(PropertiesHandler<C> delegate, NestedSetter<P, C> nestedSetter,
                RootSetter<C> rootSetter) {
            this.delegate = delegate;
            this.nestedSetter = nestedSetter;
            this.nestedNamedSetter = rootSetter;
        }

        @Override
        public C getTarget() throws PropertiesConfigReaderException {
            return delegate.getTarget();
        }

        @Override
        public boolean setOnObject(PropertyContext ctx) throws PropertiesConfigReaderException {
            return delegate.setOnObject(ctx);
        }

        @Override
        public boolean set(C t, PropertyContext ctx) throws PropertiesConfigReaderException {
            return delegate.set(t, ctx);
        }

        @Override
        public PropertiesHandler<?> getNestedHandler(String name) throws PropertiesConfigReaderException {
            return delegate.getNestedHandler(name);
        }

        @Override
        public void setNestedOnObject(Object o, String name, Object child) throws PropertiesConfigReaderException {
            delegate.setNestedOnObject(o, name, child);
        }

        @Override
        public void setNested(C t, String name, Object child) throws PropertiesConfigReaderException {
            delegate.setNested(t, name, child);
        }

        @SuppressWarnings("unchecked")
        protected void setOnParent(P t, Object c) throws PropertiesConfigReaderException {
            if (nestedSetter == null) {
                nestedNamedSetter.setRoot((C) c);
            } else {
                nestedSetter.setNested(t, (C) c);
            }
        }
    }

    protected Map<String, PropertySetter<T>> setters = Collections.emptyMap();
    protected Map<String, NestedHandlerWithSetter<T, ?>> nestedHandlers = Collections.emptyMap();

    public MappedPropertiesHandler<T> map(String name, PropertySetter<T> setter) {
        if (setters.isEmpty()) {
            setters = new HashMap<>(1);
        }
        setters.put(name, setter);
        return this;
    }

    public <C> MappedPropertiesHandler<T> map(String name, PropertiesHandler<C> nestedHandler, NestedSetter<T, C> setter) {
        return map(name, new NestedHandlerWithSetter<T, C>(nestedHandler, setter, null));
    }

    public <C> MappedPropertiesHandler<T> map(String name, PropertiesHandler<C> nestedHandler, RootSetter<C> setter) {
        return map(name, new NestedHandlerWithSetter<T, C>(nestedHandler, null, setter));
    }

    protected <C> MappedPropertiesHandler<T> map(String name, NestedHandlerWithSetter<T, C> setter) {
        if (nestedHandlers.isEmpty()) {
            nestedHandlers = new HashMap<>(1);
        }
        nestedHandlers.put(name, setter);
        return this;
    }

    @Override
    public boolean set(T t, PropertyContext ctx) throws PropertiesConfigReaderException {
        final String lastNameElement = ctx.getRelativeName();
        final PropertySetter<T> setter = setters.get(lastNameElement);
        if (setter == null) {
            return false;
        }
        setter.setProperty(t, ctx.getValue());
        return true;
    }

    @Override
    public PropertiesHandler<?> getNestedHandler(String name) throws PropertiesConfigReaderException {
        final PropertiesHandler<?> childCallback = nestedHandlers.get(name);
        if (childCallback == null) {
            return null;
        }
        return childCallback;
    }

    @Override
    public void setNested(T t, String name, Object child) throws PropertiesConfigReaderException {
        final NestedHandlerWithSetter<T, ?> nestedSetter = nestedHandlers.get(name);
        if (nestedSetter == null) {
            throw new PropertiesConfigReaderException("Failed to locate nested setter for " + name);
        }
        nestedSetter.setOnParent(t, child);
    }
}

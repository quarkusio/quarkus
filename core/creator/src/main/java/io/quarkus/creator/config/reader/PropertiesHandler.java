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

/**
 *
 * @author Alexey Loubyansky
 */
public interface PropertiesHandler<T> {

    /**
     * An instance that will receive configuration.
     *
     * @return instance that will receive configuration
     */
    T getTarget() throws PropertiesConfigReaderException;

    @SuppressWarnings("unchecked")
    default boolean setOnObject(PropertyContext ctx) throws PropertiesConfigReaderException {
        return set((T) ctx.o, ctx);
    }

    default boolean set(T t, PropertyContext ctx) throws PropertiesConfigReaderException {
        return false;
    }

    default PropertiesHandler<?> getNestedHandler(String name) throws PropertiesConfigReaderException {
        return null;
    }

    @SuppressWarnings("unchecked")
    default void setNestedOnObject(Object o, String name, Object child) throws PropertiesConfigReaderException {
        setNested((T) o, name, child);
    }

    default void setNested(T t, String name, Object child) throws PropertiesConfigReaderException {
        throw new UnsupportedOperationException(t + ", " + name + ", " + child);
    }
}

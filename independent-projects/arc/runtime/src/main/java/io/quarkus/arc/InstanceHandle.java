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

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.AlterableContext;

/**
 * Represents an instance handle.
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public interface InstanceHandle<T> extends AutoCloseable {

    /**
     *
     * @return an instance of {@code T} or {@code null}
     */
    T get();

    /**
     *
     * @return {@code true} if an instance is available, {@code false} otherwise
     */
    default boolean isAvailable() {
        return get() != null;
    }

    /**
     * Destroy the instance as defined by
     * {@link javax.enterprise.context.spi.Contextual#destroy(Object, javax.enterprise.context.spi.CreationalContext)}. If this
     * is a CDI contextual instance it is also removed from the underlying context.
     * 
     * @see AlterableContext#destroy(javax.enterprise.context.spi.Contextual)
     */
    default void destroy() {
        // No-op
    }

    /**
     *
     * @return the injectable bean for a CDI contextual instance or {@code null}
     */
    default InjectableBean<T> getBean() {
        return null;
    }

    /**
     * Delegates to {@link #destroy()} if the handle does not represent a CDI contextual instance or if it represents a
     * {@link Dependent} CDI contextual instance.
     */
    @Override
    default void close() {
        InjectableBean<T> bean = getBean();
        if (bean == null || Dependent.class.equals(bean.getScope())) {
            destroy();
        }
    }

}

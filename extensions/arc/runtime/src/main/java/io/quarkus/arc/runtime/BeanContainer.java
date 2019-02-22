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

package io.quarkus.arc.runtime;

import java.io.Closeable;
import java.lang.annotation.Annotation;

import org.jboss.quarkus.arc.ManagedContext;

/**
 * Represents a CDI bean container.
 */
public interface BeanContainer {

    /**
     * 
     * @param type
     * @param qualifiers
     * @return a bean instance or {@code null} if no matching bean is found
     */
    default <T> T instance(Class<T> type, Annotation... qualifiers) {
        return instanceFactory(type, qualifiers).create().get();
    }

    /**
     * 
     * @param type
     * @param qualifiers
     * @return a bean instance factory, never {@code null}
     */
    <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers);

    /**
     * <pre>
     * ManagedContext requestContext = beanContainer.requestContext();
     * if (requestContext.isActive()) {
     *     // Perform action 
     * } else {
     *     try {
     *         requestContext.activate();
     *         // Perform action
     *     } finally {
     *         requestContext.terminate();
     *     }
     * }
     * </pre>
     * 
     * @return the context for {@link javax.enterprise.context.RequestScoped}
     * @throws IllegalStateException If the container is not running
     */
    ManagedContext requestContext();

    interface Factory<T> {

        Factory<Object> EMPTY = new Factory<Object>() {
            @Override
            public Instance<Object> create() {
                return null;
            }
        };

        /**
         * 
         * @return a bean instance or {@code null} if no matching bean is found
         */
        Instance<T> create();
    }

    interface Instance<T> extends AutoCloseable {

        /**
         *
         * @return the underlying instance
         */
        T get();

        /**
         * releases the underlying instance
         */
        default void close() {
        };
    }
}

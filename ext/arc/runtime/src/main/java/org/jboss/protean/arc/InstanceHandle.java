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
     * Destroy/release the instance. If this is a CDI contextual instance it's also removed from the underlying context.
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
     * Delegates to {@link #destroy()}.
     */
    @Override
    default void close() {
        destroy();
    }

}

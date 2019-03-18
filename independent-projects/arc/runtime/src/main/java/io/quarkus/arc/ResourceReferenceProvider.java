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

/**
 * Makes it possible to resolve non-CDI injection points, such as Java EE resources.
 */
public interface ResourceReferenceProvider {

    /**
     * A resource reference handle is a dependent object of the object it is injected into. {@link InstanceHandle#destroy()} is
     * called when the target object is
     * destroyed.
     *
     * <pre>
     * class ResourceBean {
     *
     *     &#64;Resource(lookup = "bar")
     *     String bar;
     *
     *     &#64;Produces
     *     &#64;PersistenceContext
     *     EntityManager entityManager;
     * }
     * </pre>
     *
     * @param type
     * @param annotations
     * @return the resource reference handle or {@code null} if not resolvable
     */
    InstanceHandle<Object> get(Type type, Set<Annotation> annotations);

    /**
     * Convenient util method.
     *
     * @param annotations
     * @param annotationType
     * @return
     */
    @SuppressWarnings("unchecked")
    default <T extends Annotation> T getAnnotation(Set<Annotation> annotations, Class<T> annotationType) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(annotationType)) {
                return (T) annotation;
            }
        }
        return null;
    }

}

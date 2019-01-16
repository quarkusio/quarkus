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

package org.jboss.shamrock.arc.runtime;

import java.lang.annotation.Annotation;

public interface BeanContainer {

    default <T> T instance(Class<T> type, Annotation... qualifiers) {
        return instanceFactory(type, qualifiers).get();
    }

    <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers);

    /**
     * Runs the given action within the scope of the CDI request context
     *
     * @param action The action to run
     * @param t      The first context parameter, this is passed to the action
     * @param u      The second context parameter, this is passed to the action
     * @return the value returned by the action
     * @throws Exception
     */
    <T, U, R> R withinRequestContext(RequestAction<T, U, R> action, T t, U u) throws Exception;

    interface Factory<T> {

        T get();
    }

    interface RequestAction<T, U, R> {

        R run(T t, U u) throws Exception;
    }

}

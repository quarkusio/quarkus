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

package org.jboss.shamrock.deployment.recording;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Function;

import org.jboss.shamrock.runtime.ObjectSubstitution;
import org.jboss.shamrock.runtime.RuntimeValue;

/**
 * An injectable utility class that contains methods that can be needed for dealing with templates
 */
public interface RecorderContext {

    /**
     * Registers a way to construct an object via a non-default constructor. Each object may only have at most one
     * non-default constructor registered
     *
     * @param constructor The constructor
     * @param parameters  A function that maps the object to a list of constructor parameters
     * @param <T>         The type of the object
     */
    <T> void registerNonDefaultConstructor(Constructor<T> constructor, Function<T, List<Object>> parameters);

    /**
     * Registers a substitution to allow objects that are not serialisable to bytecode to be substituted for an object
     * that is.
     *
     * @param from         The class of the non serializable object
     * @param to           The class to serialize to
     * @param substitution The subclass of {@link ObjectSubstitution} that performs the substitution
     */
    <F, T> void registerSubstitution(Class<F> from, Class<T> to, Class<? extends ObjectSubstitution<F, T>> substitution);


    /**
     * Creates a Class instance that can be passed to a recording proxy as a substitute for a class that is not loadable
     * at processing time. At runtime the actual class will be passed into the invoked method.
     *
     * @param name The class name
     * @return A Class instance that can be passed to a recording proxy
     */
    Class<?> classProxy(String name);


    /**
     * Creates a RuntimeValue object that represents an object created via the default constructor.
     * <p>
     * This object can be passed into templates, but must not be used directly at deployment time
     *
     * @param name The name of the class
     * @param <T>  The type of the class
     * @return The class instance proxy
     */
    <T> RuntimeValue<T> newInstance(String name);
}

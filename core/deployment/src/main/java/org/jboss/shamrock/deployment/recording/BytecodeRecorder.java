package org.jboss.shamrock.deployment.recording;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Function;

import org.jboss.shamrock.runtime.InjectionInstance;

/**
 * A class that can be used to record invocations to bytecode so they can be replayed later. This is done through the
 * use of class templates and recording proxies.
 * <p>
 * A class template is simple a stateless class with a no arg constructor. This template will contain the runtime logic
 * used to bootstrap the various frameworks.
 * <p>
 * A recording proxy is a proxy of a template that records all invocations on the template, and then writes out a sequence
 * of java bytecode that performs the same invocations.
 * <p>
 * There are some limitations on what can be recorded. Only the following objects are allowed as parameters to
 * recording proxies:
 * <p>
 * - primitives
 * - String
 * - Class (see {@link #classProxy(String)} to handle classes that are not loadable at generation time)
 * - Objects with a no-arg constructor and getter/setters for all properties
 * - Any arbitrary object via the {@link #registerSubstitution(Class, Class, Class)} mechanism
 * - arrays, lists and maps of the above
 */
public interface BytecodeRecorder {

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
     * Registers a way to construct an object via a non-default constructor. Each object may only have at most one
     * non-default constructor registered
     *
     * @param constructor The constructor
     * @param parameters  A function that maps the object to a list of constructor parameters
     * @param <T>         The type of the object
     */
    <T> void registerNonDefaultConstructor(Constructor<T> constructor, Function<T, List<Object>> parameters);

    /**
     * Gets a proxy that can be used to record invocations into bytecode
     *
     * @param theClass
     * @param <T>
     * @return
     */
    <T> T getRecordingProxy(Class<T> theClass);

    /**
     * Creates a Class instance that can be passed to a recording proxy as a substitute for a class that is not loadable
     * at processing time. At runtime the actual class will be passed into the invoked method.
     *
     * @param name The class name
     * @return A Class instance that can be passed to a recording proxy
     */
    Class<?> classProxy(String name);
}

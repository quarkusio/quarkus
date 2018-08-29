package org.jboss.shamrock.deployment.codegen;

import java.io.IOException;

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
 *
 * There are some limitations on what can be recorded. Only the following objects are allowed as parameters to
 * recording proxies:
 *
 * - primitives
 * - String
 * - Class (see {@link #classProxy(String)} to handle classes that are not loadable at generation time)
 * - Objects with a no-arg constructor and getter/setters for all properties
 * - Any arbitrary object via the {@link #registerSubstitution(Class, Class, Class)} mechanism
 * - arrays, lists and maps of the above
 * 
 *
 */
public interface BytecodeRecorder extends AutoCloseable {

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
     * Creates an instance factory that can be used to create an injected instance.
     * <p>
     * The instance that is returned from this method is a proxy that can be passed into recording proxies. When this is
     * written to bytecode a functional instance will be injected into the template that can be used to create CDI injected
     * instance
     *
     * @param className The name of the class to be created
     * @return A InjectionInstance proxy that can be passed into recording proxies
     */
    InjectionInstance<?> newInstanceFactory(String className);

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

    /**
     * Close the recorder and create the bytecode
     *
     * @throws IOException
     */
    @Override
    void close() throws IOException;
}

package io.quarkus.deployment.recording;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Function;

import io.quarkus.runtime.ObjectSubstitution;
import io.quarkus.runtime.RuntimeValue;

/**
 * An injectable utility class that contains methods that can be needed for dealing with recorders.
 */
public interface RecorderContext {

    /**
     * Registers a way to construct an object via a non-default constructor. Each object may only have at most one
     * non-default constructor registered
     *
     * @param constructor The constructor
     * @param parameters A function that maps the object to a list of constructor parameters
     * @param <T> The type of the object
     */
    <T> void registerNonDefaultConstructor(Constructor<T> constructor, Function<T, List<Object>> parameters);

    /**
     * Registers a substitution to allow objects that are not serialisable to bytecode to be substituted for an object
     * that is.
     *
     * @param from The class of the non serializable object
     * @param to The class to serialize to
     * @param substitution The subclass of {@link ObjectSubstitution} that performs the substitution
     */
    <F, T> void registerSubstitution(Class<F> from, Class<T> to,
            Class<? extends ObjectSubstitution<? super F, ? super T>> substitution);

    /**
     * Register an object loader.
     *
     * @param loader the object loader (must not be {@code null})
     */
    void registerObjectLoader(ObjectLoader loader);

    /**
     * Creates a Class instance that can be passed to a recording proxy as a substitute for a class that is not loadable
     * at processing time. At runtime the actual class will be passed into the invoked method.
     *
     * @param name The class name
     * @return A Class instance that can be passed to a recording proxy
     * @deprecated This construct is no longer needed since directly loading deployment/application classes at
     *             processing time in build steps is now safe
     */
    @Deprecated
    Class<?> classProxy(String name);

    /**
     * Creates a RuntimeValue object that represents an object created via the default constructor.
     * <p>
     * This object can be passed into recorders, but must not be used directly at deployment time
     *
     * @param name The name of the class
     * @param <T> The type of the class
     * @return The class instance proxy
     */
    <T> RuntimeValue<T> newInstance(String name);
}

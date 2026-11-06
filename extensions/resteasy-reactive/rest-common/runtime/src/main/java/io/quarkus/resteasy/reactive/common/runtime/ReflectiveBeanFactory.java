package io.quarkus.resteasy.reactive.common.runtime;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.UndeclaredThrowableException;

import org.jboss.resteasy.reactive.spi.BeanFactory;

/**
 * A {@link BeanFactory} for classes that are known at build time not to be CDI beans.
 * <p>
 * It instantiates the class through its public no-arg constructor without consulting the CDI container, which avoids
 * a type-safe resolution (and the lazy materialization of the removed beans index) for every built-in serializer.
 */
public class ReflectiveBeanFactory<T> implements BeanFactory<T> {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodType VOID_TYPE = MethodType.methodType(void.class);

    private final Class<T> target;

    public ReflectiveBeanFactory(Class<T> target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "ReflectiveBeanFactory[" + target.getName() + "]";
    }

    @SuppressWarnings("unchecked")
    @Override
    public BeanInstance<T> createInstance() {
        T instance;
        try {
            ReflectiveBeanFactory.class.getModule().addReads(target.getModule());
            instance = (T) LOOKUP.findConstructor(target, VOID_TYPE).invoke();
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalArgumentException(
                    "Unable to create class '" + target.getName()
                            + "'. To fix the problem, make sure this class is a CDI bean.",
                    t instanceof RuntimeException ? t : new UndeclaredThrowableException(t));
        }
        return new BeanInstance<T>() {
            @Override
            public T getInstance() {
                return instance;
            }

            @Override
            public void close() {
            }
        };
    }
}

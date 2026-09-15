package io.quarkus.arc;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.InjectionException;

/**
 * It can be used by synthetic {@link InjectableBean} definitions to destroy a contextual instance.
 *
 * @param <T>
 * @see Contextual#destroy(Object, CreationalContext)
 */
public interface BeanDestroyer<T> {

    default void destroy(T instance, SyntheticCreationalContext<T> context) {
        destroy(instance, context, context.getParams());
    }

    /**
     *
     * @param instance
     * @param creationalContext
     * @param params
     * @deprecated Use {@link #destroy(Object, SyntheticCreationalContext)} instead
     */
    @Deprecated(forRemoval = true, since = "4.0")
    default void destroy(T instance, CreationalContext<T> creationalContext, Map<String, Object> params) {
        throw new InjectionException("Destruction logic not implemented");
    }

    /**
     * @deprecated use {@code BeanConfigurator.autoClose()} instead
     */
    @Deprecated(forRemoval = true, since = "4.0")
    class CloseableDestroyer implements BeanDestroyer<Closeable> {
        @Override
        public void destroy(Closeable instance, SyntheticCreationalContext<Closeable> creationalContext) {
            try {
                instance.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @deprecated use {@code BeanConfigurator.autoClose()} instead
     */
    @Deprecated(forRemoval = true, since = "4.0")
    class AutoCloseableDestroyer implements BeanDestroyer<AutoCloseable> {
        @Override
        public void destroy(AutoCloseable instance, SyntheticCreationalContext<AutoCloseable> creationalContext) {
            try {
                instance.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}

package io.quarkus.arc;

import jakarta.enterprise.inject.spi.Interceptor;

/**
 * Quarkus representation of an interceptor bean.
 * This interface extends the standard CDI {@link Interceptor} interface.
 *
 * @param <T>
 */
public interface InjectableInterceptor<T> extends InjectableBean<T>, Interceptor<T> {

    @Override
    default Kind getKind() {
        return Kind.INTERCEPTOR;
    }

}

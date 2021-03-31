package io.quarkus.arc;

import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.Prioritized;

/**
 * Quarkus representation of an interceptor bean.
 * This interface extends the standard CDI {@link Interceptor} interface.
 *
 * @param <T>
 */
public interface InjectableInterceptor<T> extends InjectableBean<T>, Interceptor<T>, Prioritized {

    @Override
    default Kind getKind() {
        return Kind.INTERCEPTOR;
    }

}

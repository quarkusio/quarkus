package io.quarkus.arc;

import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.Prioritized;

/**
 * Represents an interceptor bean.
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public interface InjectableInterceptor<T> extends InjectableBean<T>, Interceptor<T>, Prioritized {

}

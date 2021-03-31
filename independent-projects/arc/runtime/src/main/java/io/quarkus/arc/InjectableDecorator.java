package io.quarkus.arc;

import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Prioritized;

/**
 * Quarkus representation of a decorator bean.
 * This interface extends the standard CDI {@link Decorator} interface.
 */
public interface InjectableDecorator<T> extends InjectableBean<T>, Decorator<T>, Prioritized {

    @Override
    default Kind getKind() {
        return Kind.DECORATOR;
    }

}

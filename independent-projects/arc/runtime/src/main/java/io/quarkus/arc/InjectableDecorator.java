package io.quarkus.arc;

import jakarta.enterprise.inject.spi.Decorator;

/**
 * Quarkus representation of a decorator bean.
 * This interface extends the standard CDI {@link Decorator} interface.
 */
public interface InjectableDecorator<T> extends InjectableBean<T>, Decorator<T> {

    @Override
    default Kind getKind() {
        return Kind.DECORATOR;
    }

}

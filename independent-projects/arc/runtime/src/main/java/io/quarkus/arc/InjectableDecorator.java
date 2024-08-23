package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.enterprise.inject.spi.Decorator;

import io.quarkus.arc.impl.Qualifiers;

/**
 * Quarkus representation of a decorator bean.
 * This interface extends the standard CDI {@link Decorator} interface.
 */
public interface InjectableDecorator<T> extends InjectableBean<T>, Decorator<T> {

    @Override
    default Kind getKind() {
        return Kind.DECORATOR;
    }

    @Override
    default Set<Annotation> getDelegateQualifiers() {
        return Qualifiers.DEFAULT_QUALIFIERS;
    }
}

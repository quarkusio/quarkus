package io.quarkus.mongodb.panache.common.validation;

import java.util.Optional;
import java.util.Set;

public class NoopEntityValidator<V, E extends RuntimeException> implements EntityValidator<V, E> {
    @Override
    public Set<? extends V> validate(Object entity) {
        return Set.of();
    }

    @Override
    public Optional<E> toException(Set<? extends V> violations) {
        return Optional.empty();
    }
}

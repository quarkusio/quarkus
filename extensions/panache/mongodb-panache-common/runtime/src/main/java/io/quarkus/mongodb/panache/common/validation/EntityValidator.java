package io.quarkus.mongodb.panache.common.validation;

import java.util.Optional;
import java.util.Set;

public interface EntityValidator<V, E extends RuntimeException> {

    Set<? extends V> validate(Object entity);

    Optional<E> toException(Set<? extends V> violations);

    class Holder {
        public static EntityValidator<?, ? extends RuntimeException> INSTANCE;
    }
}

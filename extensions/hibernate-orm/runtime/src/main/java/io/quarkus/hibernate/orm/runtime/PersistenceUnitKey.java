package io.quarkus.hibernate.orm.runtime;

import java.util.Objects;

/**
 * We implement equals/hashCode here as this element is used in a Map and the initialization
 * of the hashCode method for the record is actually quite slow, even with Project Leyden
 * (at least in its current version).
 * <p>
 * Hopefully, we will be able to simplify this in the future.
 */
public record PersistenceUnitKey(String name, boolean isReactive) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PersistenceUnitKey that = (PersistenceUnitKey) o;
        return Objects.equals(name, that.name) && isReactive == that.isReactive;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(name);
        result = 31 * result + Boolean.hashCode(isReactive);
        return result;
    }
}

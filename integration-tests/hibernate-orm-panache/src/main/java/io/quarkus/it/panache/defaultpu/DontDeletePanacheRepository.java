package io.quarkus.it.panache.defaultpu;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

/**
 * Intermediate interface that overrides {@code deleteAll()} with a default method
 * that throws, to test that Panache bytecode enhancement does not overwrite
 * default method overrides defined in superinterfaces.
 */
public interface DontDeletePanacheRepository<E, I> extends PanacheRepositoryBase<E, I> {

    String PROHIBIT_DELETE_MESSAGE = "You shall not delete!";

    @Override
    default long deleteAll() {
        throw new UnsupportedOperationException(PROHIBIT_DELETE_MESSAGE);
    }
}

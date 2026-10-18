package io.quarkus.it.panache.defaultpu;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

/**
 * Abstract superclass that overrides {@code deleteAll()} with a concrete method
 * that throws, to test that Panache bytecode enhancement does not overwrite
 * concrete method overrides defined in superclasses.
 */
public abstract class AbstractDontDeletePanacheRepository<E, I> implements PanacheRepositoryBase<E, I> {

    public static final String PROHIBIT_DELETE_MESSAGE = "You shall not delete from superclass!";

    @Override
    public long deleteAll() {
        throw new UnsupportedOperationException(PROHIBIT_DELETE_MESSAGE);
    }
}

package io.quarkus.data.hibernate.managed.blocking;

import java.util.stream.Stream;

import org.hibernate.Session;

import io.quarkus.data.hibernate.managed.ManagedRepositoryOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheBlockingOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheOperations;

public interface BlockingManagedRepositoryOperations<Entity, Id>
        extends ManagedRepositoryOperations<Entity, Session, Void, Boolean, Id> {

    // At build time, a bytecode transformer rewrites getEntityClass() to call doGetEntityClass()
    // via invokeinterface (instead of invokespecial), and makes doGetEntityClass() public.
    // The generated repository implementation then overrides doGetEntityClass() to return the
    // concrete entity class. This avoids the fragile AbstractJpaOperations.getRepositoryEntityClass(getClass())
    // map lookup, which breaks when ArC generates a _Subclass for intercepted repositories.
    // Both methods stay private in source so they don't leak into the user-facing API.

    private Class<? extends Entity> doGetEntityClass() {
        throw new UnsupportedOperationException(
                "doGetEntityClass() should be provided by the generated repository implementation");
    }

    private Class<? extends Entity> getEntityClass() {
        return doGetEntityClass();
    }

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingManaged();
    }

    // Operations

    @Override
    default Session getSession() {
        return operations().getSession(getEntityClass());
    }

    @Override
    default Void persist(Entity entity) {
        return operations().persist(entity);
    }

    @Override
    default Void persistAndFlush(Entity entity) {
        return operations().persistAndFlush(entity);
    }

    @Override
    default Void delete(Entity entity) {
        return operations().delete(entity);
    }

    @Override
    default Boolean isPersistent(Entity entity) {
        return operations().isPersistent(entity);
    }

    @Override
    default Void flush() {
        return operations().flush(getEntityClass());
    }

    @Override
    default Void persist(Iterable<Entity> entities) {
        return operations().persist(entities);
    }

    @Override
    default Void persist(Stream<Entity> entities) {
        return operations().persist(entities);
    }

    @Override
    default Void persist(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return operations().persist(firstEntity, entities);
    }
}

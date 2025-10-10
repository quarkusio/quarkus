package io.quarkus.hibernate.panache.stateless.blocking;

import io.quarkus.hibernate.panache.PanacheRepositorySwitcher;

public interface PanacheStatelessBlockingRepositoryBase<Entity, Id>
        extends PanacheStatelessBlockingRepositoryOperations<Entity, Id>,
        PanacheStatelessBlockingRepositoryQueries<Entity, Id>,
        PanacheRepositorySwitcher<Entity, Id> {

}

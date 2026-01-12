package io.quarkus.hibernate.panache.managed.blocking;

import io.quarkus.hibernate.panache.PanacheRepositorySwitcher;

public interface PanacheManagedBlockingRepositoryBase<Entity, Id>
        extends PanacheManagedBlockingRepositoryOperations<Entity, Id>,
        PanacheManagedBlockingRepositoryQueries<Entity, Id>,
        PanacheRepositorySwitcher<Entity, Id> {

}

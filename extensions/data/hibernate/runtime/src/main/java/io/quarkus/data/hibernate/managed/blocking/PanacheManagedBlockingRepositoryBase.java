package io.quarkus.data.hibernate.managed.blocking;

import io.quarkus.data.hibernate.PanacheRepositorySwitcher;

public interface PanacheManagedBlockingRepositoryBase<Entity, Id>
        extends PanacheManagedBlockingRepositoryOperations<Entity, Id>,
        PanacheManagedBlockingRepositoryQueries<Entity, Id>,
        PanacheRepositorySwitcher<Entity, Id> {

}

package io.quarkus.data.hibernate.managed.blocking;

import io.quarkus.data.hibernate.RepositorySwitcher;

public interface PanacheManagedBlockingRepositoryBase<Entity, Id>
        extends PanacheManagedBlockingRepositoryOperations<Entity, Id>,
        PanacheManagedBlockingRepositoryQueries<Entity, Id>,
        RepositorySwitcher<Entity, Id> {

}

package io.quarkus.data.hibernate.managed.blocking;

import io.quarkus.data.hibernate.RepositorySwitcher;

public interface BlockingManagedRepositoryBase<Entity, Id>
        extends BlockingManagedRepositoryOperations<Entity, Id>,
        BlockingManagedRepositoryQueries<Entity, Id>,
        RepositorySwitcher<Entity, Id> {

}

package io.quarkus.data.hibernate.managed.reactive;

import io.quarkus.data.hibernate.RepositorySwitcher;

public interface ReactiveManagedRepositoryBase<Entity, Id>
        extends ReactiveManagedRepositoryOperations<Entity, Id>,
        ReactiveManagedRepositoryQueries<Entity, Id>,
        RepositorySwitcher<Entity, Id> {

}

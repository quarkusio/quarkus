package io.quarkus.data.hibernate.stateless.blocking;

import io.quarkus.data.hibernate.RepositorySwitcher;

public interface PanacheStatelessBlockingRepositoryBase<Entity, Id>
        extends PanacheStatelessBlockingRepositoryOperations<Entity, Id>,
        PanacheStatelessBlockingRepositoryQueries<Entity, Id>,
        RepositorySwitcher<Entity, Id> {

}

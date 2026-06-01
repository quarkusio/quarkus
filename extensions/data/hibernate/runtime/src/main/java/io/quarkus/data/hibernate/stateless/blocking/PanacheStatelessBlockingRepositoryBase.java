package io.quarkus.data.hibernate.stateless.blocking;

import io.quarkus.data.hibernate.PanacheRepositorySwitcher;

public interface PanacheStatelessBlockingRepositoryBase<Entity, Id>
        extends PanacheStatelessBlockingRepositoryOperations<Entity, Id>,
        PanacheStatelessBlockingRepositoryQueries<Entity, Id>,
        PanacheRepositorySwitcher<Entity, Id> {

}

package io.quarkus.hibernate.panache.managed.reactive;

import io.quarkus.hibernate.panache.PanacheRepositorySwitcher;

public interface PanacheManagedReactiveRepositoryBase<Entity, Id>
        extends PanacheManagedReactiveRepositoryOperations<Entity, Id>,
        PanacheManagedReactiveRepositoryQueries<Entity, Id>,
        PanacheRepositorySwitcher<Entity, Id> {

}

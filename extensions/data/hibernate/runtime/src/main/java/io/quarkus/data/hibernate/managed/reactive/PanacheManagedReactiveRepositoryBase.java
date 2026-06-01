package io.quarkus.data.hibernate.managed.reactive;

import io.quarkus.data.hibernate.PanacheRepositorySwitcher;

public interface PanacheManagedReactiveRepositoryBase<Entity, Id>
        extends PanacheManagedReactiveRepositoryOperations<Entity, Id>,
        PanacheManagedReactiveRepositoryQueries<Entity, Id>,
        PanacheRepositorySwitcher<Entity, Id> {

}

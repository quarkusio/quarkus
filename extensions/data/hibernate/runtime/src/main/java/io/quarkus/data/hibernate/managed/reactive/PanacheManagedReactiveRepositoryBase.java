package io.quarkus.data.hibernate.managed.reactive;

import io.quarkus.data.hibernate.RepositorySwitcher;

public interface PanacheManagedReactiveRepositoryBase<Entity, Id>
        extends PanacheManagedReactiveRepositoryOperations<Entity, Id>,
        PanacheManagedReactiveRepositoryQueries<Entity, Id>,
        RepositorySwitcher<Entity, Id> {

}

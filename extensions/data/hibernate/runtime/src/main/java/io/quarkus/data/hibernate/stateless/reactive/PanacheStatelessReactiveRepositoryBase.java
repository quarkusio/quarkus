package io.quarkus.data.hibernate.stateless.reactive;

import io.quarkus.data.hibernate.RepositorySwitcher;

public interface PanacheStatelessReactiveRepositoryBase<Entity, Id>
        extends PanacheStatelessReactiveRepositoryOperations<Entity, Id>,
        PanacheStatelessReactiveRepositoryQueries<Entity, Id>,
        RepositorySwitcher<Entity, Id> {

}

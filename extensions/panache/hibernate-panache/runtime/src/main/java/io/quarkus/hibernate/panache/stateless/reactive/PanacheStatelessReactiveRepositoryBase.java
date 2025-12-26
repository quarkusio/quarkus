package io.quarkus.hibernate.panache.stateless.reactive;

import io.quarkus.hibernate.panache.PanacheRepositorySwitcher;

public interface PanacheStatelessReactiveRepositoryBase<Entity, Id>
        extends PanacheStatelessReactiveRepositoryOperations<Entity, Id>,
        PanacheStatelessReactiveRepositoryQueries<Entity, Id>,
        PanacheRepositorySwitcher<Entity, Id> {

}

package io.quarkus.data.hibernate.stateless.reactive;

import io.quarkus.data.hibernate.PanacheRepositorySwitcher;

public interface PanacheStatelessReactiveRepositoryBase<Entity, Id>
        extends PanacheStatelessReactiveRepositoryOperations<Entity, Id>,
        PanacheStatelessReactiveRepositoryQueries<Entity, Id>,
        PanacheRepositorySwitcher<Entity, Id> {

}

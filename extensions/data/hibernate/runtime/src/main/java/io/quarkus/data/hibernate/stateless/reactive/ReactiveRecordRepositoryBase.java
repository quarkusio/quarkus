package io.quarkus.data.hibernate.stateless.reactive;

import io.quarkus.data.hibernate.RepositorySwitcher;

public interface ReactiveRecordRepositoryBase<Entity, Id>
        extends ReactiveRecordRepositoryOperations<Entity, Id>,
        ReactiveRecordRepositoryQueries<Entity, Id>,
        RepositorySwitcher<Entity, Id> {

}

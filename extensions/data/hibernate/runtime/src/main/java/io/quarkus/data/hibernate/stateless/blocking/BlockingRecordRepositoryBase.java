package io.quarkus.data.hibernate.stateless.blocking;

import io.quarkus.data.hibernate.RepositorySwitcher;

public interface BlockingRecordRepositoryBase<Entity, Id>
        extends BlockingRecordRepositoryOperations<Entity, Id>,
        BlockingRecordRepositoryQueries<Entity, Id>,
        RepositorySwitcher<Entity, Id> {

}

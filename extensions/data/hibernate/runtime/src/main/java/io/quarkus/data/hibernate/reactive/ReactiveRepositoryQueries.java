package io.quarkus.data.hibernate.reactive;

import java.util.List;

import io.quarkus.data.hibernate.RepositoryQueries;
import io.smallrye.mutiny.Uni;

public interface ReactiveRepositoryQueries<Entity, Id>
        extends
        RepositoryQueries<Uni<Entity>, Uni<List<Entity>>, ReactiveDataQuery<Entity>, Uni<Long>, Uni<Boolean>, Id> {
}

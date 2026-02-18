package io.quarkus.hibernate.panache.reactive;

import java.util.List;

import io.quarkus.hibernate.panache.PanacheRepositoryQueries;
import io.smallrye.mutiny.Uni;

public interface PanacheRepositoryReactiveQueries<Entity, Id>
        extends
        PanacheRepositoryQueries<Uni<Entity>, Uni<List<Entity>>, PanacheReactiveQuery<Entity>, Uni<Long>, Uni<Boolean>, Id> {
}

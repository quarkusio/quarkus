package io.quarkus.data.hibernate.reactive;

import java.util.List;

import io.quarkus.data.hibernate.PanacheRepositoryQueries;
import io.smallrye.mutiny.Uni;

public interface PanacheRepositoryReactiveQueries<Entity, Id>
        extends
        PanacheRepositoryQueries<Uni<Entity>, Uni<List<Entity>>, PanacheReactiveQuery<Entity>, Uni<Long>, Uni<Boolean>, Id> {
}

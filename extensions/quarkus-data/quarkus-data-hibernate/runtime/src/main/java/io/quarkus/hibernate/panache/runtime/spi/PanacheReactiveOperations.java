package io.quarkus.hibernate.panache.runtime.spi;

import java.util.List;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.panache.reactive.PanacheReactiveQuery;
import io.smallrye.mutiny.Uni;

public interface PanacheReactiveOperations
        extends
        PanacheOperations<Uni<Object>, Uni<List<?>>, PanacheReactiveQuery<?>, Uni<Long>, Uni<Void>, Uni<Boolean>> {

    Uni<Mutiny.Session> getSession(Class<?> entityClass);

    Uni<Mutiny.StatelessSession> getStatelessSession(Class<?> entityClass);

}

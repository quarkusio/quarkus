package io.quarkus.data.hibernate.runtime.spi;

import java.util.List;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.data.hibernate.reactive.PanacheReactiveQuery;
import io.smallrye.mutiny.Uni;

public interface PanacheReactiveOperations
        extends
        PanacheOperations<Uni<Object>, Uni<List<?>>, PanacheReactiveQuery<?>, Uni<Long>, Uni<Void>, Uni<Boolean>> {

    Uni<Mutiny.Session> getSession(Class<?> entityClass);

    Uni<Mutiny.StatelessSession> getStatelessSession(Class<?> entityClass);

}

package io.quarkus.hibernate.reactive.panache.runtime;

import java.util.List;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class JpaOperations extends AbstractJpaOperations<PanacheQueryImpl<?>> {
    public static final JpaOperations INSTANCE = new JpaOperations();

    @Override
    protected PanacheQueryImpl<?> createPanacheQuery(Uni<Mutiny.Session> session, String query, String orderBy,
            Object paramsArrayOrMap) {
        return new PanacheQueryImpl<>(session, query, orderBy, paramsArrayOrMap);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Uni<List<?>> list(PanacheQueryImpl<?> query) {
        return (Uni) query.list();
    }

    @Override
    protected Multi<?> stream(PanacheQueryImpl<?> query) {
        return query.stream();
    }
}

package io.quarkus.hibernate.panache.runtime.hr;

import java.util.List;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.panache.reactive.PanacheReactiveQuery;
import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractStatelessJpaOperations;
import io.smallrye.mutiny.Uni;

public class StatelessReactiveJpaOperations extends AbstractStatelessJpaOperations<PanacheReactiveQuery<?>> {

    @Override
    protected PanacheReactiveQuery<?> createPanacheQuery(Uni<Mutiny.StatelessSession> session, String query,
            String originalQuery,
            String orderBy, Object paramsArrayOrMap) {
        return new PanacheStatelessReactiveQueryImpl<>(session, query, originalQuery, orderBy, paramsArrayOrMap);
    }

    @Override
    public Uni<List<?>> list(PanacheReactiveQuery<?> query) {
        return (Uni) query.list();
    }
}

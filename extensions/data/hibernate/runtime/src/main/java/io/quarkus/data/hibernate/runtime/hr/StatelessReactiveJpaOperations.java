package io.quarkus.data.hibernate.runtime.hr;

import java.util.List;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.data.hibernate.reactive.PanacheReactiveQuery;
import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractStatelessJpaOperations;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

public class StatelessReactiveJpaOperations extends AbstractStatelessJpaOperations<PanacheReactiveQuery<?>> {

    @Override
    protected PanacheReactiveQuery<?> createPanacheQuery(Uni<Mutiny.StatelessSession> session, Class<?> entityClass,
            String query,
            String originalQuery,
            Sort sort, Object paramsArrayOrMap) {
        return new PanacheStatelessReactiveQueryImpl<>(session, entityClass, query, originalQuery, sort, paramsArrayOrMap);
    }

    @Override
    public Uni<List<?>> list(PanacheReactiveQuery<?> query) {
        return (Uni) query.list();
    }
}

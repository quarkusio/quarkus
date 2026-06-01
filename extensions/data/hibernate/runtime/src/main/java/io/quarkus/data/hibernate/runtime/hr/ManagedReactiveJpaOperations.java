package io.quarkus.data.hibernate.runtime.hr;

import java.util.List;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.data.hibernate.reactive.ReactiveDataQuery;
import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractManagedJpaOperations;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

public class ManagedReactiveJpaOperations extends AbstractManagedJpaOperations<ReactiveDataQuery<?>> {

    @Override
    protected ReactiveDataQuery<?> createPanacheQuery(Uni<Mutiny.Session> session, Class<?> entityClass, String query,
            String originalQuery,
            Sort sort, Object paramsArrayOrMap) {
        return new PanacheManagedReactiveQueryImpl<>(session, entityClass, query, originalQuery, sort, paramsArrayOrMap);
    }

    @Override
    public Uni<List<?>> list(ReactiveDataQuery<?> query) {
        return (Uni) query.list();
    }
}

package io.quarkus.hibernate.reactive.panache.runtime;

import java.util.List;

import org.hibernate.query.SelectionQuery;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractManagedJpaOperations;
import io.quarkus.hibernate.reactive.panache.common.runtime.CommonManagedPanacheQueryImpl;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

public class JpaOperations extends AbstractManagedJpaOperations<PanacheQueryImpl<?>> {

    public static final JpaOperations INSTANCE = new JpaOperations();

    @Override
    protected PanacheQueryImpl<?> createPanacheQuery(Uni<Mutiny.Session> session, Class<?> entityClass, String query,
            String originalQuery,
            Sort sort,
            Object paramsArrayOrMap) {
        return new PanacheQueryImpl<>(session, entityClass, query, originalQuery, sort, paramsArrayOrMap);
    }

    @Override
    protected PanacheQueryImpl<?> createPanacheQuery(Uni<Mutiny.Session> session, Class<?> entityClass,
            Uni<? extends SelectionQuery<?>> prebuiltQuery) {
        return new PanacheQueryImpl<>(
                new CommonManagedPanacheQueryImpl<>(session, entityClass,
                        (Uni<? extends Mutiny.SelectionQuery<?>>) prebuiltQuery));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Uni<List<?>> list(PanacheQueryImpl<?> query) {
        return (Uni) query.list();
    }

}

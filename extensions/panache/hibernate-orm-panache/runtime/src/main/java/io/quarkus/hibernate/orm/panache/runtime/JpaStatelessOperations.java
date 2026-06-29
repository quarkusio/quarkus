package io.quarkus.hibernate.orm.panache.runtime;

import java.util.List;
import java.util.stream.Stream;

import org.hibernate.StatelessSession;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractStatelessJpaOperations;
import io.quarkus.panache.common.Sort;

public class JpaStatelessOperations extends AbstractStatelessJpaOperations<PanacheQueryImpl<?>> {
    /**
     * Provides the default implementations for quarkus to wire up. Should not be used by third party developers.
     */
    public static final JpaStatelessOperations INSTANCE = new JpaStatelessOperations();

    @Override
    protected PanacheQueryImpl<?> createPanacheQuery(StatelessSession session, Class<?> entityClass, String query,
            String originalQuery,
            Sort sort,
            Object paramsArrayOrMap) {
        return new PanacheQueryImpl<>(session, entityClass, query, originalQuery, sort, paramsArrayOrMap);
    }

    @Override
    public List<?> list(PanacheQueryImpl<?> query) {
        return query.list();
    }

    @Override
    public Stream<?> stream(PanacheQueryImpl<?> query) {
        return query.stream();
    }

}

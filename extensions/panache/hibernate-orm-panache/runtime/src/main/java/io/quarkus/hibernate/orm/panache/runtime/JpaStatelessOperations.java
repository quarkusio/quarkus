package io.quarkus.hibernate.orm.panache.runtime;

import java.util.List;
import java.util.stream.Stream;

import org.hibernate.StatelessSession;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractStatelessJpaOperations;

public class JpaStatelessOperations extends AbstractStatelessJpaOperations<PanacheQueryImpl<?>> {
    /**
     * Provides the default implementations for quarkus to wire up. Should not be used by third party developers.
     */
    public static final JpaStatelessOperations INSTANCE = new JpaStatelessOperations();

    @Override
    protected PanacheQueryImpl<?> createPanacheQuery(StatelessSession session, String query, String originalQuery,
            String orderBy,
            Object paramsArrayOrMap) {
        return new PanacheQueryImpl<>(session, query, originalQuery, orderBy, paramsArrayOrMap);
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

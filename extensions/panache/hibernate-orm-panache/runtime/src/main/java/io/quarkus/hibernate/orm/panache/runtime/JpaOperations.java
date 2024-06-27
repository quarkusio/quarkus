package io.quarkus.hibernate.orm.panache.runtime;

import java.util.List;
import java.util.stream.Stream;

import org.hibernate.Session;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;

public class JpaOperations extends AbstractJpaOperations<PanacheQueryImpl<?>> {
    /**
     * Provides the default implementations for quarkus to wire up. Should not be used by third party developers.
     */
    public static final JpaOperations INSTANCE = new JpaOperations();

    @Override
    protected PanacheQueryImpl<?> createPanacheQuery(Session session, String query, String originalQuery, String orderBy,
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

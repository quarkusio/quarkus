package io.quarkus.hibernate.orm.panache.runtime;

import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;

public class JpaOperations extends AbstractJpaOperations<PanacheQueryImpl<?>> {
    /**
     * Provides the default implementations for quarkus to wire up. Should not be used by third party developers.
     */
    public static final JpaOperations INSTANCE = new JpaOperations();

    @Override
    protected PanacheQueryImpl<?> createPanacheQuery(EntityManager em, String query, String orderBy,
            Object paramsArrayOrMap) {
        return new PanacheQueryImpl<>(em, query, orderBy, paramsArrayOrMap);
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

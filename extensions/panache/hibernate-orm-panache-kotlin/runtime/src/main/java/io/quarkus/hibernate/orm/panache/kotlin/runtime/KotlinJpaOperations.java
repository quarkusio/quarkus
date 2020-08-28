package io.quarkus.hibernate.orm.panache.kotlin.runtime;

import java.util.List;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;

public class KotlinJpaOperations extends AbstractJpaOperations<PanacheQueryImpl<?>> {
    /**
     * Provides the default implementations for quarkus to wire up. Should not be used by third party developers.
     */
    public static final KotlinJpaOperations INSTANCE = new KotlinJpaOperations();

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

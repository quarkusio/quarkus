package io.quarkus.hibernate.panache.runtime.orm;

import java.util.List;
import java.util.stream.Stream;

import org.hibernate.StatelessSession;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractStatelessJpaOperations;
import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;

public class StatelessBlockingJpaOperations extends AbstractStatelessJpaOperations<PanacheBlockingQuery<?>> {

    @Override
    protected PanacheBlockingQuery<?> createPanacheQuery(StatelessSession session, Class<?> entityClass, String query,
            String originalQuery,
            String orderBy, Object paramsArrayOrMap) {
        return new PanacheBlockingQueryImpl<>(session, entityClass, query, originalQuery, orderBy, paramsArrayOrMap);
    }

    @Override
    public List<?> list(PanacheBlockingQuery<?> query) {
        return query.list();
    }

    @Override
    public Stream<?> stream(PanacheBlockingQuery<?> query) {
        return query.stream();
    }

}

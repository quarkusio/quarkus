package io.quarkus.hibernate.panache.runtime.orm;

import java.util.List;
import java.util.stream.Stream;

import org.hibernate.Session;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractManagedJpaOperations;
import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;

public class ManagedBlockingJpaOperations extends AbstractManagedJpaOperations<PanacheBlockingQuery<?>> {

    @Override
    protected PanacheBlockingQuery<?> createPanacheQuery(Session session, String query, String originalQuery,
            String orderBy, Object paramsArrayOrMap) {
        return new PanacheBlockingQueryImpl<>(session, query, originalQuery, orderBy, paramsArrayOrMap);
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

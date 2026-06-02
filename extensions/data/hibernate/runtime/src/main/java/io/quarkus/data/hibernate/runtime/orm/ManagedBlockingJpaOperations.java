package io.quarkus.data.hibernate.runtime.orm;

import java.util.List;
import java.util.stream.Stream;

import org.hibernate.Session;

import io.quarkus.data.hibernate.blocking.PanacheBlockingQuery;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractManagedJpaOperations;
import io.quarkus.panache.common.Sort;

public class ManagedBlockingJpaOperations extends AbstractManagedJpaOperations<PanacheBlockingQuery<?>> {

    @Override
    protected PanacheBlockingQuery<?> createPanacheQuery(Session session, Class<?> entityClass, String query,
            String originalQuery,
            Sort sort, Object paramsArrayOrMap) {
        return new PanacheBlockingQueryImpl<>(session, entityClass, query, originalQuery, sort, paramsArrayOrMap);
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

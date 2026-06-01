package io.quarkus.data.hibernate.runtime.orm;

import java.util.List;
import java.util.stream.Stream;

import io.quarkus.panache.common.Sort;
import org.hibernate.StatelessSession;

import io.quarkus.data.hibernate.blocking.PanacheBlockingQuery;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractStatelessJpaOperations;

public class StatelessBlockingJpaOperations extends AbstractStatelessJpaOperations<PanacheBlockingQuery<?>> {

    @Override
    protected PanacheBlockingQuery<?> createPanacheQuery(StatelessSession session, Class<?> entityClass, String query,
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

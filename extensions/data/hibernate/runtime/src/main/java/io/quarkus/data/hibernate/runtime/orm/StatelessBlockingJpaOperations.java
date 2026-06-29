package io.quarkus.data.hibernate.runtime.orm;

import java.util.List;
import java.util.stream.Stream;

import org.hibernate.StatelessSession;

import io.quarkus.data.hibernate.blocking.BlockingDataQuery;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractStatelessJpaOperations;
import io.quarkus.panache.common.Sort;

public class StatelessBlockingJpaOperations extends AbstractStatelessJpaOperations<BlockingDataQuery<?>> {

    @Override
    protected BlockingDataQuery<?> createPanacheQuery(StatelessSession session, Class<?> entityClass, String query,
            String originalQuery,
            Sort sort, Object paramsArrayOrMap) {
        return new PanacheBlockingQueryImpl<>(session, entityClass, query, originalQuery, sort, paramsArrayOrMap);
    }

    @Override
    public List<?> list(BlockingDataQuery<?> query) {
        return query.list();
    }

    @Override
    public Stream<?> stream(BlockingDataQuery<?> query) {
        return query.stream();
    }

}

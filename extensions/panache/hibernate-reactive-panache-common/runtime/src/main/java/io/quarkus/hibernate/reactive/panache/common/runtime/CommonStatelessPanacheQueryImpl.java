package io.quarkus.hibernate.reactive.panache.common.runtime;

import org.hibernate.Filter;
import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

public class CommonStatelessPanacheQueryImpl<Entity> extends CommonAbstractPanacheQueryImpl<Entity, Mutiny.StatelessSession> {

    protected Uni<Mutiny.StatelessSession> em;

    public CommonStatelessPanacheQueryImpl(Uni<Mutiny.StatelessSession> em, String query, String originalQuery, String orderBy,
            Object paramsArrayOrMap) {
        super(em, query, originalQuery, orderBy, paramsArrayOrMap);
    }

    protected CommonStatelessPanacheQueryImpl(CommonStatelessPanacheQueryImpl<?> previousQuery, String newQueryString,
            String customCountQueryForSpring, Class<?> projectionType) {
        super(previousQuery, newQueryString, customCountQueryForSpring, projectionType);
    }

    @Override
    public <NewEntity> CommonStatelessPanacheQueryImpl<NewEntity> project(Class<NewEntity> type) {
        return (CommonStatelessPanacheQueryImpl<NewEntity>) super.project(type);
    }

    @Override
    protected <T> CommonAbstractPanacheQueryImpl<T, Mutiny.StatelessSession> newQuery(String query,
            String customCountQueryForSpring, Class<T> type) {
        return new CommonStatelessPanacheQueryImpl<>(this, query, customCountQueryForSpring, type);
    }

    @Override
    protected Filter enableFilter(Mutiny.StatelessSession session, String filter) {
        // FIXME
        throw new UnsupportedOperationException("Not supported yet upstream");
        //        return session.enableFilter(filter);
    }

    @Override
    protected void disableFilter(Mutiny.StatelessSession session, String filter) {
        // FIXME
        throw new UnsupportedOperationException("Not supported yet upstream");
        //        session.disableFilter(filter);
    }
}

package io.quarkus.hibernate.reactive.panache.common.runtime;

import org.hibernate.Filter;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

public class CommonManagedPanacheQueryImpl<Entity> extends CommonAbstractPanacheQueryImpl<Entity, Mutiny.Session> {

    public CommonManagedPanacheQueryImpl(Uni<Mutiny.Session> em, Class<?> entityClass, String query, String originalQuery,
            Sort sort,
            Object paramsArrayOrMap) {
        super(em, entityClass, query, originalQuery, sort, paramsArrayOrMap);
    }

    protected CommonManagedPanacheQueryImpl(CommonManagedPanacheQueryImpl<?> previousQuery, String newQueryString,
            String customCountQueryForSpring, Class<?> projectionType) {
        super(previousQuery, newQueryString, customCountQueryForSpring, projectionType);
    }

    @Override
    public <NewEntity> CommonManagedPanacheQueryImpl<NewEntity> project(Class<NewEntity> type) {
        return (CommonManagedPanacheQueryImpl<NewEntity>) super.project(type);
    }

    @Override
    protected <T> CommonAbstractPanacheQueryImpl<T, Mutiny.Session> newQuery(String query, String customCountQueryForSpring,
            Class<T> type) {
        return new CommonManagedPanacheQueryImpl<>(this, query, customCountQueryForSpring, type);
    }

    @Override
    protected Filter enableFilter(Mutiny.Session session, String filter) {
        return session.enableFilter(filter);
    }

    @Override
    protected void disableFilter(Mutiny.Session session, String filter) {
        session.disableFilter(filter);
    }
}

package io.quarkus.hibernate.orm.panache.runtime;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.query.internal.QueryImpl;

import io.quarkus.hibernate.orm.panache.common.runtime.CommonPanacheQueryImpl;

//TODO this class is only needed by the Spring Data JPA module and would be placed there it it weren't for a dev-mode classloader issue
// see https://github.com/quarkusio/quarkus/issues/6214
public class CustomCountPanacheQuery<Entity> extends PanacheQueryImpl<Entity> {

    public CustomCountPanacheQuery(EntityManager em, Query jpaQuery, String customCountQuery,
            Object paramsArrayOrMap) {
        super(new CommonPanacheQueryImpl<>(em, castQuery(jpaQuery).getQueryString(), null, paramsArrayOrMap) {
            {
                this.countQuery = customCountQuery;
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private static QueryImpl castQuery(Query jpaQuery) {
        if (!(jpaQuery instanceof QueryImpl)) {
            throw new IllegalArgumentException("Unexpected Query class: '" + jpaQuery.getClass().getName() + "', where '"
                    + QueryImpl.class.getName() + "' is expected.");
        }
        return (QueryImpl) jpaQuery;
    }
}

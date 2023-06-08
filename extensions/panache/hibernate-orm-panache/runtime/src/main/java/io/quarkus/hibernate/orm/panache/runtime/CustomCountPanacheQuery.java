package io.quarkus.hibernate.orm.panache.runtime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.hibernate.query.spi.AbstractQuery;

import io.quarkus.hibernate.orm.panache.common.runtime.CommonPanacheQueryImpl;

//TODO this class is only needed by the Spring Data JPA module and would not be placed there if it weren't for a dev-mode classloader issue
// see https://github.com/quarkusio/quarkus/issues/6214
public class CustomCountPanacheQuery<Entity> extends PanacheQueryImpl<Entity> {

    public CustomCountPanacheQuery(EntityManager em, Query jpaQuery, String customCountQuery,
            Object paramsArrayOrMap) {
        super(new CommonPanacheQueryImpl<>(em, castQuery(jpaQuery).getQueryString(), null, null, paramsArrayOrMap) {
            {
                this.countQuery = customCountQuery;
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private static org.hibernate.query.sqm.internal.QuerySqmImpl castQuery(Query jpaQuery) {
        if (!(jpaQuery instanceof org.hibernate.query.sqm.internal.QuerySqmImpl)) {
            throw new IllegalArgumentException("Unexpected Query class: '" + jpaQuery.getClass().getName() + "', where '"
                    + AbstractQuery.class.getName() + "' is expected.");
        }
        return (org.hibernate.query.sqm.internal.QuerySqmImpl) jpaQuery;
    }
}

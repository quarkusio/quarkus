package io.quarkus.hibernate.orm.panache.runtime;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import io.quarkus.hibernate.orm.panache.common.runtime.CommonPanacheQueryImpl;

//TODO this class is only needed by the Spring Data JPA module and would be placed there it it weren't for a dev-mode classloader issue
// see https://github.com/quarkusio/quarkus/issues/6214
public class CustomCountPanacheQuery<Entity> extends PanacheQueryImpl<Entity> {

    public CustomCountPanacheQuery(EntityManager em, Query jpaQuery, String query, String customCountQuery,
            Object paramsArrayOrMap) {
        super(new CommonPanacheQueryImpl<Entity>(em, query, null, paramsArrayOrMap) {
            {
                this.countQuery = customCountQuery;
            }
        });
    }
}

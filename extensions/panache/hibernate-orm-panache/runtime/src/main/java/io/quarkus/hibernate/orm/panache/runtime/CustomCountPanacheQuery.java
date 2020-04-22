package io.quarkus.hibernate.orm.panache.runtime;

import javax.persistence.EntityManager;
import javax.persistence.Query;

//TODO this class is only needed by the Spring Data JPA module and would be placed there it it weren't for a dev-mode classloader issue
// see https://github.com/quarkusio/quarkus/issues/6214
public class CustomCountPanacheQuery<Entity> extends PanacheQueryImpl<Entity> {

    private final String customCountQuery;

    public CustomCountPanacheQuery(EntityManager em, Query jpaQuery, String query, String customCountQuery,
            Object paramsArrayOrMap) {
        super(em, query, null, paramsArrayOrMap);
        this.customCountQuery = customCountQuery;
    }

    @Override
    protected String countQuery() {
        return customCountQuery;
    }
}

package io.quarkus.hibernate.orm.panache.runtime;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class CustomCountPanacheQuery<Entity> extends PanacheQueryImpl<Entity> {

    private final String customCountQuery;

    public CustomCountPanacheQuery(EntityManager em, Query jpaQuery, String query, String customCountQuery,
            Object paramsArrayOrMap) {
        super(em, jpaQuery, query, paramsArrayOrMap);
        this.customCountQuery = customCountQuery;
    }

    @Override
    protected String countQuery() {
        return customCountQuery;
    }
}

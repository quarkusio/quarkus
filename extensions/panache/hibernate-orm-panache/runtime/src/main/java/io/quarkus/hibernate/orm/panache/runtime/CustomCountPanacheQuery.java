package io.quarkus.hibernate.orm.panache.runtime;

import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;

import io.quarkus.hibernate.orm.panache.common.runtime.CommonPanacheQueryImpl;

//TODO this class is only needed by the Spring Data JPA module and would not be placed there if it weren't for a dev-mode classloader issue
// see https://github.com/quarkusio/quarkus/issues/6214
public class CustomCountPanacheQuery<Entity> extends PanacheQueryImpl<Entity> {

    public CustomCountPanacheQuery(Session session, SelectionQuery hibernateQuery, String customCountQuery,
            Object paramsArrayOrMap) {
        super(new CommonPanacheQueryImpl<>(session, CommonPanacheQueryImpl.getQueryString(hibernateQuery),
                null, null, paramsArrayOrMap) {
            {
                this.customCountQueryForSpring = customCountQuery;
            }
        });
    }
}

package io.quarkus.hibernate.reactive.panache.runtime;

import java.util.Map;

import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

//TODO this class is only needed by the Spring Data JPA module and would be placed there it it weren't for a dev-mode classloader issue
// see https://github.com/quarkusio/quarkus/issues/6214
public class AdditionalJpaOperations {

    @SuppressWarnings({ "rawtypes" })
    public static PanacheQuery<?> find(Class<?> entityClass, String query, String countQuery, Sort sort,
            Map<String, Object> params) {
        String findQuery = JpaOperations.createFindQuery(entityClass, query, JpaOperations.paramCount(params));
        return new CustomCountPanacheQuery(JpaOperations.getSession(), findQuery, countQuery, params);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, String countQuery, Sort sort,
            Parameters parameters) {
        return find(entityClass, query, countQuery, sort, parameters.map());
    }

    @SuppressWarnings({ "rawtypes" })
    public static PanacheQuery<?> find(Class<?> entityClass, String query, String countQuery, Sort sort, Object... params) {
        String findQuery = JpaOperations.createFindQuery(entityClass, query, JpaOperations.paramCount(params));
        return new CustomCountPanacheQuery(JpaOperations.getSession(), findQuery, countQuery, params);
    }
}

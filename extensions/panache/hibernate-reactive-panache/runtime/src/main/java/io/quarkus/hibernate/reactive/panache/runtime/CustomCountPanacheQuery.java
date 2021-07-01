package io.quarkus.hibernate.reactive.panache.runtime;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.common.runtime.CommonPanacheQueryImpl;
import io.smallrye.mutiny.Uni;

//TODO this class is only needed by the Spring Data JPA module and would be placed there it it weren't for a dev-mode classloader issue
// see https://github.com/quarkusio/quarkus/issues/6214
public class CustomCountPanacheQuery<Entity> extends PanacheQueryImpl<Entity> {

    public CustomCountPanacheQuery(Uni<Mutiny.Session> em, String query, String customCountQuery,
            Object paramsArrayOrMap) {
        super(new CommonPanacheQueryImpl<Entity>(em, query, null, paramsArrayOrMap) {
            {
                this.countQuery = customCountQuery;
            }
        });
    }
}

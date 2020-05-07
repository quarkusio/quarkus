package io.quarkus.it.panache.rest.repository;

import javax.ws.rs.core.Response;

import io.quarkus.panache.rest.common.PanacheRestResource;
import io.quarkus.panache.rest.hibernate.orm.PanacheRepositoryCrudResource;

public interface AuthorPojoRepositoryCrudResource
        extends PanacheRepositoryCrudResource<AuthorPojoRepository, AuthorPojo, Long> {

    @PanacheRestResource(exposed = false)
    Response add(AuthorPojo entity);

    @PanacheRestResource(exposed = false)
    Response update(Long id, AuthorPojo entity);

    @PanacheRestResource(exposed = false)
    void delete(Long id);
}

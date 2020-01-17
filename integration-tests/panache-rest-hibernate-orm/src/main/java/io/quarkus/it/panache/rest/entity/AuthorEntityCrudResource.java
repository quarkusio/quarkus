package io.quarkus.it.panache.rest.entity;

import javax.ws.rs.core.Response;

import io.quarkus.panache.rest.common.PanacheRestResource;
import io.quarkus.panache.rest.hibernate.orm.PanacheEntityCrudResource;

public interface AuthorEntityCrudResource extends PanacheEntityCrudResource<AuthorEntity, Long> {

    @PanacheRestResource(exposed = false)
    Response add(AuthorEntity entity);

    @PanacheRestResource(exposed = false)
    Response update(Long id, AuthorEntity entity);

    @PanacheRestResource(exposed = false)
    void delete(Long id);
}

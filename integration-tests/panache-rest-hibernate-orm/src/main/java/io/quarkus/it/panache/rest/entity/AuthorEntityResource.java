package io.quarkus.it.panache.rest.entity;

import javax.ws.rs.core.Response;

import io.quarkus.panache.rest.common.OperationProperties;
import io.quarkus.panache.rest.hibernate.orm.PanacheEntityCrudResource;

public interface AuthorEntityResource extends PanacheEntityCrudResource<AuthorEntity, Long> {

    @OperationProperties(exposed = false)
    Response add(AuthorEntity entity);

    @OperationProperties(exposed = false)
    Response update(Long id, AuthorEntity entity);

    @OperationProperties(exposed = false)
    void delete(Long id);
}

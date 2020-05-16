package io.quarkus.it.hibernate.orm.rest.data.panache.entity;

import javax.ws.rs.core.Response;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.MethodProperties;

public interface AuthorEntityResource extends PanacheEntityResource<AuthorEntity, Long> {

    @MethodProperties(exposed = false)
    Response add(AuthorEntity entity);

    @MethodProperties(exposed = false)
    Response update(Long id, AuthorEntity entity);

    @MethodProperties(exposed = false)
    void delete(Long id);
}

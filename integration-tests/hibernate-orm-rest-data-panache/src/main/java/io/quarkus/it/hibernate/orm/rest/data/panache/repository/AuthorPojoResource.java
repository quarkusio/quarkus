package io.quarkus.it.hibernate.orm.rest.data.panache.repository;

import javax.ws.rs.core.Response;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.MethodProperties;

public interface AuthorPojoResource
        extends PanacheRepositoryResource<AuthorPojoRepository, AuthorPojo, Long> {

    @MethodProperties(exposed = false)
    Response add(AuthorPojo entity);

    @MethodProperties(exposed = false)
    Response update(Long id, AuthorPojo entity);

    @MethodProperties(exposed = false)
    void delete(Long id);
}

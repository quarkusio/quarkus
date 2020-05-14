package io.quarkus.it.panache.rest.repository;

import javax.ws.rs.core.Response;

import io.quarkus.panache.rest.common.OperationProperties;
import io.quarkus.panache.rest.hibernate.orm.PanacheRepositoryCrudResource;

public interface AuthorPojoResource
        extends PanacheRepositoryCrudResource<AuthorPojoRepository, AuthorPojo, Long> {

    @OperationProperties(exposed = false)
    Response add(AuthorPojo entity);

    @OperationProperties(exposed = false)
    Response update(Long id, AuthorPojo entity);

    @OperationProperties(exposed = false)
    void delete(Long id);
}
